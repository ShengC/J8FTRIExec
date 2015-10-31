package chapter1;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Exercise {
	public static File[] listDirectory(File root) {
		return root.listFiles(f -> f.isDirectory());
	}
	
	public static File[] listFiles(File root, String ext) {
		return root.listFiles((f, nm) -> f.isFile() && nm.endsWith(ext));
	}
	
	public static File[] sortFiles(File[] files) {
		Arrays.sort(files, (f1, f2) -> {
			int i;
			if (f1.isDirectory() && f2.isFile()) i = -1;
			else if (f1.isFile() && f1.isDirectory()) i = 1;
			else i = f1.getName().compareTo(f2.getName());
			return i;
		});
		
		return files;
	}
	
	interface RunnableEx {
		public void run() throws Exception;
		public static Runnable uncheck(RunnableEx runner) {
			return () -> { 
				try { runner.run(); }
				catch (Exception e) { throw new RuntimeException(e); }
			};
		}
	}
	
	public static Runnable andThen(Runnable r1, Runnable r2) {
		return () -> { r1.run(); r2.run(); };
	}
	
	interface Collection2<T> extends Collection<T> {
		default void forEachIf(Consumer<T> action, Predicate<T> filter) {
			this.stream().filter(filter).forEach(action);
		}
	}
	
	public static void main(String... args) {
		// just to test compile
		new Thread(RunnableEx.uncheck(() -> { System.err.println("blah"); Thread.sleep(1000); }));
		
		andThen(() -> System.err.println("foo"), () -> System.err.println("bar"));
	}
}
