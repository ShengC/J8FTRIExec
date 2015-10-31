package chapter3;

import java.util.function.Supplier;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public class Exercise {
	interface Logger {
		default Level getLevel() { return Level.FINEST; }
		public void log(String message);
		default void logIf(Level level, Supplier<String> message) {
			if (level.equals(getLevel()))
				log(message.get());
		}
	}
	
	public static <T> T withLock(ReentrantLock lock, Supplier<T> action) {
		lock.lock();
		try {
			return action.get();
		}
		finally {
			lock.unlock();
		}
	}
	
	public static Image transform(Image in, ColorTransformer f) {
		int width = (int) in.getWidth();
		int height = (int) in.getHeight();
		WritableImage out = new WritableImage(width, height);
		
		Stream.iterate(0, x -> x + 1).limit(width).forEach(x -> {
			Stream.iterate(0, y -> y + 1).limit(height).forEach(y -> {
				out.getPixelWriter().setColor(x, y, f.apply(x, y, in.getPixelReader().getColor(x, y)));
			});
		});
		
		return out;
	} 
	
	public static Image addFrame(Image in) {
		int width = (int) in.getWidth();
		int height = (int) in.getHeight();
		return transform(in, (x, y, c) -> {
			if (x <= 10 || x >= width - 10 || y <= 10 || y >= height - 10) return Color.GRAY;
			else return c;
		});
	}
	
	public static <T> Image transform(Image in, BiFunction<Color, T, Color> f, T arg) {
		int width = (int) in.getWidth();
		int height = (int) in.getHeight();
		WritableImage out = new WritableImage(width, height);
		
		Stream.iterate(0, x -> x + 1).limit(width).forEach(x -> {
			Stream.iterate(0, y -> y + 1).limit(height).forEach(y -> {
				Color c = in.getPixelReader().getColor(x, y);
				out.getPixelWriter().setColor(x, y, f.apply(c, arg));
			});
		});
		
		return out;
	}
	
	public static Comparator<String> produceComparator(BiFunction<String, String, Integer>... fs) {
		return (a, b) -> Stream.of(fs).map(f -> f.apply(a, b)).reduce(0, (x, y) -> {
			if (x != 0) return x;
			else return y;
		});
	}
	
	public static Image genericAddFrame(Image in, int span, Color c) {
		int width = (int) in.getWidth();
		int height = (int) in.getHeight();
		return transform(in, (x, y, cc) -> {
			if (x <= span || x >= (width - span) || y <= span || y >= (height - span)) return c;
			else return cc;
		});
	}
	
	public static Comparator<Object> lexigraphicComparator(String... fieldNames) {
		return (x, y) -> {
			Class<?> cx = x.getClass();
			Class<?> cy = y.getClass();
			
			for (String f : fieldNames) {
				Object vx = null; 
				Object vy = null; 
				try { vx = cx.getField(f).get(x); } 
				catch (Exception IGNORE) {}
				try { vy = cy.getField(f).get(y); }
				catch (Exception IGNORE) {}
				
				// you can also compare `toString()` here,
				int hx = System.identityHashCode(vx);
				int hy = System.identityHashCode(vy);
				
				if (hx != hy) return hx - hy;
			}
			
			return 0;
		};
	}
	
	public static ColorTransformer compose(ColorTransformer c1, ColorTransformer c2) {
		return (x, y, c) -> { return c2.apply(x, y, c1.apply(x, y, c)); };
	}
	
	public static ColorTransformer toColorTransformer(UnaryOperator<Color> u) {
		return (x, y, c) -> u.apply(c);
	}
	
	static class LatentImage {
		private Image in;
		private List<ColorTransformer> pendingOperations;
		
		private LatentImage(Image in) { this.in = in; }
		
		LatentImage transform(ColorTransformer f) {
			pendingOperations.add(f);
			return this;
		}
		
		LatentImage transform(UnaryOperator<Color> f) {
			return transform(toColorTransformer(f));
		}
		
		public static LatentImage fromImage(Image in) { return new LatentImage(in); }
		
		public Image toImage() {
			ColorTransformer c = pendingOperations.stream().reduce((x, y, color) -> color, (x, y) -> compose(x, y));
			
			return Exercise.transform(in, c);
		}
	}
	
	static class LatentImage2 {
		private Image in;
		private List<ColorTransformer2> pendingOperations;
		private LatentImage2(Image in) { this.in = in; }
		
		LatentImage2 transform(ColorTransformer2 f) {
			pendingOperations.add(f);
			return this;
		}
		
		LatentImage2 transform(UnaryOperator<Color> u) {
			return transform(ColorTransformer2.toColorTransformer2(u));
		}
		
		public static LatentImage2 fromImage(Image in) { return new LatentImage2(in); }
		
		public Image toImage() {
			int width = (int)in.getWidth();
			int height = (int)in.getHeight();

			Color[][] cache = new Color[width][height];			
			PixelReader reader = in.getPixelReader();
			ColorReader cached = (x, y) -> {
				if (cache[x][y] == null) { cache[x][y] = reader.getColor(x, y); }
				return cache[x][y];
			};
			
			WritableImage out = new WritableImage(width, height);
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					Color c = cached.getColor(i, j);
					for (ColorTransformer2 op : pendingOperations) {
						c = op.apply(i, j, cached);
						cache[i][j] = c;
					}
					out.getPixelWriter().setColor(i, j, c);
				}
			}
			
			return out;
		}
		
		public Image toImageParallel() throws Exception {
			final int N = Runtime.getRuntime().availableProcessors();
			final CountDownLatch latch = new CountDownLatch(1);
			final CyclicBarrier barrier = new CyclicBarrier(N, () -> latch.countDown());
			
			final int W = (int)in.getWidth();
			final int H = (int)in.getHeight();
			final Color[][] cache = new Color[W][H]; // always update at disjoint index among threads, so it is safe
			final ColorReader reader = (x, y) -> {
				if (cache[x][y] == null) { cache[x][y] = in.getPixelReader().getColor(x, y); }
				return cache[x][y];
			};
			
			final WritableImage out = new WritableImage(W, H);
			for (int n = 0; n < N; n++) {
				final int fromY = n * H / N;
				final int toY = (n + 1) * H / N;
				new Thread(
					() -> {
						for (int i = 0; i < W; i++)
							for (int j = fromY; j < toY; j++) {
								Color c = reader.getColor(i, j);
								for (ColorTransformer2 op : pendingOperations) {
									c = op.apply(i, j, reader);
									cache[i][j] = c;
								}
								
								out.getPixelWriter().setColor(i, j, c);
							}
						
						try { barrier.await(); }
						catch (Exception e) { Thread.currentThread().interrupt(); }
					}
				).start();
			}
			
			latch.await();
			return out;
		}
	}
	
	public static <T> void doInOrderAsync(final Supplier<T> fst, final BiConsumer<T, Throwable> snd) {
		new Thread(() -> {
			try { snd.accept(fst.get(), null); }
			catch (Throwable t) { snd.accept(null, t); }
		}).start();
	}
	
	public static void doInParallelAsync(final Runnable fst, final Runnable snd, final Consumer<Throwable> consumer) {
		new Thread(() -> {
			try { fst.run(); }
			catch (Throwable t) { consumer.accept(t); }
		}).start();
		
		new Thread(() -> {
			try { snd.run(); }
			catch (Throwable t) { consumer.accept(t); }
		}).start();
	}
	
	public static <T,U> Function<T,U> unchecked(FunctionCheckedException<T,U> function) {
		return t -> {
			try { return function.apply(t); }
			catch (Exception e) { throw new RuntimeException(e); }
		};
	}
	
	public static <T,U> List<U> map(List<T> list, Function<T,U> f) {
		final List<U> mapped = new LinkedList<>();
		list.stream().forEach(t -> mapped.add(f.apply(t)));
		return mapped;
	}
	
	public static <T,U> Future<U> map(Future<T> future, Function<T,U> f) {
		return new Future<U>() {
			public boolean cancel(boolean mayInterrupted) { return future.cancel(mayInterrupted); }
			public boolean isDone() { return future.isDone(); }
			public boolean isCancelled() { return future.isCancelled(); }
			public U get() throws InterruptedException, ExecutionException { return f.apply(future.get()); }
			public U get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException { return f.apply(future.get(timeout, unit)); }
		};
	}
	
	public static <T,U> Pair<U> map(Pair<T> pair, Function<T,U> f) {
		return Pair.pair(f.apply(pair.getLeft()), f.apply(pair.getRight()));
	}
}

enum Level { FINEST, INFO, WARN, DEBUG, TRACE }

@FunctionalInterface
interface ColorTransformer {
	Color apply(int x, int y, Color colorAtXY);
}

interface ColorReader {
	public Color getColor(int x, int y);
}

@FunctionalInterface
interface ColorTransformer2 {
	Color apply(int x, int y, ColorReader reader);
	
	static public ColorTransformer2 toColorTransformer2(UnaryOperator<Color> u) {
		return (x, y, r) -> u.apply(r.getColor(x, y));
	}
	
	static public ColorTransformer2 frame(int span, int width, int height, Color color) {
		return (x, y, reader) -> {
			if (x <= span || x >= (width - span) || y <= span || (height - y) <= span) 
				return reader.getColor(x, y);
			else
				return color;
		};
	}
	
	static final ColorTransformer2 BRIGHTEN = 
		(x, y, reader) -> reader.getColor(x, y).brighter();
		
	static final ColorTransformer2 EDGE = 
		(x, y, reader) -> {
			Color n = reader.getColor(x, y - 1);
			Color s = reader.getColor(x, y + 1);
			Color w = reader.getColor(x - 1, y);
			Color e = reader.getColor(x + 1, y);
			Color o = reader.getColor(x, y);
			
			double r = 4 * o.getRed() - (n == null ? 0.0 : n.getRed()) - (s == null ? 0.0 : s.getRed()) - (w == null ? 0.0 : w.getRed()) - (e == null ? 0.0 : e.getRed());
			double g = 4 * o.getGreen() - (n == null ? 0.0 : n.getGreen()) - (s == null ? 0.0 : s.getGreen()) - (w == null ? 0.0 : w.getGreen()) - (e == null ? 0.0 : e.getGreen());
			double b = 4 * o.getBlue() - (n == null ? 0.0 : n.getBlue()) - (s == null ? 0.0 : s.getBlue()) - (w == null ? 0.0 : w.getBlue()) - (e == null ? 0.0 : e.getBlue());
			
			return Color.rgb((int)r, (int)g, (int)b);
		};
}

@FunctionalInterface
interface FunctionCheckedException<T,U> {
	public U apply(T t) throws Exception;
}

interface Pair<T> {
	public T getLeft();
	public T getRight();
	
	public static <U> Pair<U> pair(final U left, final U right) {
		return new Pair<U>() {
			public U getLeft() { return left; }
			public U getRight() { return right; }
		};
	}
}