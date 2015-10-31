package chapter8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.checkedQueue;

public class Exercise {
	public static long add(int a, int b) {
		return Integer.toUnsignedLong(a) + Integer.toUnsignedLong(b);
	}
	
	public static long substract(int a, int b) {
		return Integer.toUnsignedLong(a) - Integer.toUnsignedLong(b);
	}
	
	public static long multiply(int a, int b) {
		return Integer.toUnsignedLong(a) * Integer.toUnsignedLong(b);
	}	
	
	public static long divide(int a, int b) {
		return Long.divideUnsigned(Integer.toUnsignedLong(a), Integer.toUnsignedLong(b));
	}
	
	public static int compare(int a, int b) {
		return Long.compareUnsigned(Integer.toUnsignedLong(a), Integer.toUnsignedLong(b));
	}
	
	public static int countShortWords(List<String> words) {		
		words.removeIf(w -> w.length() >= 12);
		return words.size();
	}
	
	public static <T extends Comparable<? super T>> Comparator<T> getReversedNaturalOrderComparator() {
		return Comparator.nullsLast(Comparator.reverseOrder());
	} 
			
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void showBenefitOfCheckedQueue() {
		Queue queue = checkedQueue(new LinkedList<Path>(), Path.class);
		queue.add(new File("a.txt").toPath());
		queue.add(new File("b.txt").toPath());
		// runtime error
		queue.add("c.txt");
	}
	
	public static Stream<String> turnScannerIntoStreamOfWords(final Scanner scanner) {
        Iterator<String> iter = new Iterator<String>() {
            String nextLine = null;

            @Override
            public boolean hasNext() {
                if (nextLine != null) {
                    return true;
                } else {
                    try {
                        nextLine = scanner.nextLine();
                        return true;
                    } catch (NoSuchElementException e) {
                    	nextLine = null;
                    	return false;
                    } 
                }
            }

            @Override
            public String next() {
                if (nextLine != null || hasNext()) {
                    String line = nextLine;
                    nextLine = null;
                    return line;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                iter, Spliterator.ORDERED | Spliterator.NONNULL), false);		
	}
	
	public static Stream<Path> findTransientAndVolatile(Path root) throws IOException {
		return Files.walk(root, FileVisitOption.FOLLOW_LINKS).filter(path -> { 
			try { 
				return Files.lines(path).anyMatch(line -> { return line.indexOf("volatile") >= 0 || line.indexOf("transient") >= 0; }); 
			}
			catch (IOException e) {
				return false;
			}
		});
	}
	
	public static String[] getContent(URL url, String user, String password) throws IOException {
		String encoded = Base64.getEncoder().encodeToString(String.format("%s:%s", user, password).getBytes(StandardCharsets.UTF_8));
		URLConnection connection = url.openConnection();
		connection.connect();
		connection.setRequestProperty("Authorization", "Basic " + encoded);
		try (
			InputStream stream = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		) {
			return reader.lines().toArray(String[]::new);
		}
	}
	
	public static void main(String... args) {
		showBenefitOfCheckedQueue();
	}
}

interface Point {
	public int x();
	public int y();
	
	public static final Comparator<Point> COMPARATOR = 
			Comparator.comparing(Point::x).thenComparing(Point::y);
}

interface Rectangle {
	public Point ul();
	public Point ur();
	public Point ll();
	public Point lr();
	
	public static final Comparator<Rectangle> COMPARATOR = 
			Comparator
			.comparing(Rectangle::ul, Point.COMPARATOR)
			.thenComparing(Rectangle::ur, Point.COMPARATOR)
			.thenComparing(Rectangle::ll, Point.COMPARATOR)
			.thenComparing(Rectangle::lr, Point.COMPARATOR);
}

