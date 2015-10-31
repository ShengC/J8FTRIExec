package chapter2;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;
import java.util.Queue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.LongStream;
import java.util.stream.Collectors;

public class Exercise {
	public static int countWords(String[] words) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final Queue<Integer> queue = new LinkedBlockingQueue<>();
		
		final int np = Runtime.getRuntime().availableProcessors();
		
		final CyclicBarrier barrier = new CyclicBarrier(np, () -> latch.countDown());
		
		final int len = words.length;
		final int step = len / np;		
		int st = 0;
		int en = (st + step < len) ? (st + step) : len;
		for (int i = 0; i < np; i++) {
			final int start = st;
			
			new Thread(() -> {
				long c = Stream.of(words).skip(start).limit(step).filter(w -> w.length() > 12).count();
				queue.add((int)c);
				try { barrier.await(); }
				catch (Exception IGNORE) { Thread.currentThread().interrupt(); }
			}).start();
			
			st = en;
			en = (st + step < len) ? (st + step) : len;
		}
		
		latch.await();
		return queue.stream().reduce(0, (x, y) -> x + y);
	}
	
	public static LongStream randomized(long seed, long a, long c, long m) {
		return LongStream.iterate(seed, r -> (a * r + c) % m);
	}
	
	public static LongStream randomized(long seed) {
		return randomized(seed, 25214903917L, 11L, (long)Math.pow(2, 48));
	}
	
	public static Stream<Character> characterStream(String str) {
		return Stream.iterate(0, i -> i + 1).limit(str.length()).map(str::charAt);
	} 
	
	public static <T> Stream<T> zip(Stream<T> first, Stream<T> second) {
		Iterator<T> iterF = first.iterator();
		Iterator<T> iterS = second.iterator();
		
		Stream.Builder<T> builder = Stream.builder();
		while (iterF.hasNext() && iterS.hasNext()) {
			builder.accept(iterF.next());
			builder.accept(iterS.next());
		}
		return builder.build();
	}
	
	public static <T> ArrayList<T> join1(Stream<ArrayList<T>> stream) {
		return stream.reduce((x, y) -> {
			ArrayList<T> array = new ArrayList<>();
			array.addAll(x);
			array.addAll(y);
			return array;
		}).orElse(new ArrayList<T>());
	}
	
	public static <T> ArrayList<T> join2(Stream<ArrayList<T>> stream) {
		return stream.reduce(new ArrayList<T>(), (x, y) -> { x.addAll(y); return x; });
	}
	
	public static <T> ArrayList<T> join3(Stream<ArrayList<T>> stream) {
		return stream.reduce(new ArrayList<T>(), (x, y) -> { x.addAll(y); return x; }, (x, y) -> {
			ArrayList<T> array = new ArrayList<>();
			array.addAll(x);
			array.addAll(y);
			return array;
		});
	}
	
	public static double average(Stream<Double> stream) {
		AtomicInteger c = new AtomicInteger(0);
		return stream.reduce(0.0, (x, y) -> (x * c.getAndIncrement() + y) / c.get());
	}
	
	public static String parallelCountWords(Stream<String> words) {
		AtomicInteger[] counts = Stream.generate(() -> new AtomicInteger()).limit(12).toArray(AtomicInteger[]::new);
		words.parallel().forEach(w -> {
			if (w.length() < 12) counts[w.length()].getAndIncrement();
		});
		Integer[] ints = Stream.of(counts).map(x -> x.get()).toArray(Integer[]::new);
		return Arrays.toString(ints);
	}
	
	public static String parallelCountWords2(Stream<String> words) {
		long[] counts = new long[12];
		
		words.
		parallel().
		filter(x -> x.length() < 12).
		collect(Collectors.groupingBy(x -> x.length(), Collectors.counting())).
		forEach((k, v) -> counts[k] = v);
		
		return Arrays.toString(counts);
	}
	
	public static void main(String... args) {
		//zip(Stream.of(1,2,3), Stream.of(4,5,6)).forEach(System.out::println);
		//System.out.println(average(Stream.of(1.0, 2.0, 3.0, 7.0)));
		//characterStream("Hello World!").forEach(System.out::println);
	}
}
