package chapter6;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class Exercise {
	public static String getLongestString(AtomicReference<String> record, String seen) {
		return record.accumulateAndGet(seen, (existing, observed) -> (existing == null) ? observed : (existing.length() < observed.length() ? existing : observed));
	}
	
	public static void comparePerformance() throws Exception {
		final int N = 1000;
		final int M = 100_000;
		
		final AtomicLong counterOne = new AtomicLong(0L);
		final CountDownLatch latchOne = new CountDownLatch(1);
		final CyclicBarrier barrierOne = new CyclicBarrier(N, () -> latchOne.countDown());
		
		Instant startOne = Instant.now();
		for (int i = 0; i < N; i++)
			new Thread(() -> {
				for (int j = 0; j < M; j++) counterOne.incrementAndGet();
				try { barrierOne.await(); }
				catch (Exception e) { Thread.currentThread().interrupt(); }
			}).start();
		
		latchOne.await();
		long valueOne = counterOne.get();
		Instant endOne = Instant.now();
		System.out.println("with AtomicLong.incrementAndGet, final value is " + valueOne + ", and it took " + Duration.between(startOne, endOne).toNanos() + " nano seconds");
		
		final LongAdder counterTwo = new LongAdder();
		final CountDownLatch latchTwo = new CountDownLatch(1);
		final CyclicBarrier barrierTwo = new CyclicBarrier(N, () -> latchTwo.countDown());
		
		Instant startTwo = Instant.now();
		for (int i = 0; i < N; i++)
			new Thread(() -> {
				for (int j = 0; j < M; j++) counterTwo.increment();
				try { barrierTwo.await(); }
				catch (Exception e) { Thread.currentThread().interrupt(); }
			}).start();
		
		latchTwo.await();
		long valueTwo = counterOne.get();
		Instant endTwo = Instant.now();
		System.out.println("with LongAdder.increment, final value is " + valueTwo + ", and it took " + Duration.between(startTwo, endTwo).toNanos() + " nano seconds");
	}
	
	public static long getMax(long... inputs) {
		LongAccumulator accum = new LongAccumulator(Math::max, Long.MIN_VALUE);
		LongStream.of(inputs).parallel().forEach(accum::accumulate);
		return accum.get();
	}
	
	public static long getMin(long... inputs) {
		LongAccumulator accum = new LongAccumulator(Math::min, Long.MAX_VALUE);
		LongStream.of(inputs).parallel().forEach(accum::accumulate);
		return accum.get();
	}	
	
	public ConcurrentHashMap<String, Set<File>> calInvertedWordIndex(File... files) {
		ConcurrentHashMap<String, Set<File>> map = new ConcurrentHashMap<>();
		Stream.of(files).parallel().forEach(f -> { // .parallel() mimics multiple threads reading the files
			try {
				Files.lines(f.toPath()).forEach(line -> {
					Stream.of(line.split("\\s+")).forEach(w -> {
						map.merge(w, Collections.singleton(f), (existing, more) -> { existing.addAll(more); return existing; });
					});
				});
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		
		return map;
	}
	
	public ConcurrentHashMap<String, Set<File>> calInvertedWordIndex2(File... files) {
		ConcurrentHashMap<String, Set<File>> map = new ConcurrentHashMap<>();
		Stream.of(files).parallel().forEach(f -> { // .parallel() mimics multiple threads reading the files
			try {
				Files.lines(f.toPath()).forEach(line -> {
					Stream.of(line.split("\\s+")).forEach(w -> {
						map.computeIfAbsent(w, k -> Collections.singleton(f)).add(f);
					});
				});
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		
		return map;
	}	
	
	public String findKeyWithMaximumValueInMap(ConcurrentHashMap<String, Long> map) {
		return map.reduceEntries(1, (x, y) -> x.getValue() > y.getValue() ? x : y).getKey();
	}
	
	public static int[] calFibonacci(int n) {
		Matrix[] ms = new Matrix[n];
		Arrays.parallelSetAll(ms, i -> Matrix.matrix(2, 1, 1, 1, 0));
		Arrays.parallelPrefix(ms, 0, n, Matrix::multiply);
		return Stream.of(ms).mapToInt(m -> m.apply(0,0)).toArray();
	} 
	
	public static void paralleldisplatLinks() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		CompletableFuture
		.supplyAsync(() -> {
			System.out.println("enter a url:");
			try { return new URL(new Scanner(System.in).nextLine()); }
			catch (Exception e) { latch.countDown(); throw new RuntimeException(e); }
		})
		.thenApply(url -> {
			List<String> list = new LinkedList<>();
			try ( BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream())); ) {
				for (String line; (line = reader.readLine()) != null;) list.add(line);
				return list;
			}
			catch (Exception e) {
				latch.countDown();
				throw new RuntimeException(e);
			}
		})
		.thenAccept(list -> {
			Pattern pattern = Pattern.compile("<a href=\"([^\"]*)\"*");
			for (String line : list) {
				Matcher matcher = pattern.matcher(line);
				while (matcher.find()) System.out.println("link: " + matcher.group(1));
			}
			latch.countDown();
		});
		
		latch.await();
	}
	
	public static <T> CompletableFuture<T> repeat(Supplier<T> action, Predicate<T> until) {
		return CompletableFuture.supplyAsync(action).thenCompose(t -> {
			if (until.test(t)) return CompletableFuture.supplyAsync(() -> t);
			else return repeat(action, until);
		});
	} 
	
	public static void main(String... args) throws Exception {
		//comparePerformance();
		paralleldisplatLinks();
	}
}

class Matrix {
	private final int[][] data;
	private final int N;
	
	private Matrix(int[][] data) {
		assert(data.length == data[0].length);
		this.data = data;
		this.N = data.length;
	}
	
	public Matrix multiply(Matrix that) {
		assert(this.N == that.N);
		
		int[][] result = new int[N][N];
		for (int i = 0; i < N; i++)
			for (int j = 0; j < N; j++)
				for (int k = 0; k < N; k++)
					result[i][j] += this.data[i][k] * that.data[k][j];
		return new Matrix(result);
	}
	
	public int apply(int x, int y) {
		return data[x][y];
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[\n");
		for (int i = 0; i < N; i++) 
			sb.
			append("\t").
			append(String.join(",", IntStream.of(data[i]).mapToObj(String::valueOf).toArray(String[]::new))).
			append("\n");
		sb.append("]\n");
		return sb.toString();
	}
	
	public static Matrix matrix(final int n, final int... data) {
		int[][] matrix = new int[n][n];
		int c = 0;
		for (int i = 0; i < n; i++)
			for (int j = 0; j < n; j++)
				matrix[i][j] = data[c++];
		return new Matrix(matrix);
	}
}
