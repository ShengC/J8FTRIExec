package chapter5;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Exercise {
	public static LocalDate calProgrammersDay() {
		return LocalDate.ofYearDay(LocalDate.now().getYear(), 256);
	}
	
	public static TemporalAdjuster next(Predicate<LocalDate> pred) {
		return TemporalAdjusters.ofDateAdjuster(d -> {
			LocalDate tmp = d;
			do {
				tmp = tmp.plusDays(1);
			} while (pred.test(tmp));
			
			return tmp;
		});
	}
	
	public static long getDaysAlive(LocalDate date) {
		return date.until(LocalDate.now(), ChronoUnit.DAYS);
	}
	
	public static void listAll13thFridayIn20thCentury() {
		LocalDate now = LocalDate.of(1900, 1, 1);
		
		for (int i = 0; i < 100; i++) {
			LocalDate then = now;
			for (int j = 0; j < 12; j++) {
				LocalDate check = then.plusDays(12);
				if (check.getDayOfWeek().getValue() == 5)
					System.out.println(check);
				then = then.plusMonths(1);
			}

			now = now.plusYears(1);
		}
	}
	
	public static Integer[] getAllOffsets() {
		return ZoneId.getAvailableZoneIds().stream().map(ZoneId::of).map(z -> ZonedDateTime.of(LocalDateTime.now(), z).getOffset().getTotalSeconds()).toArray(Integer[]::new);
	}
	
	public static String[] getZones() {
		return ZoneId.getAvailableZoneIds().stream().filter(d -> ZonedDateTime.of(LocalDateTime.now(), ZoneId.of(d)).getOffset().getTotalSeconds() % 3600 != 0L).toArray(String[]::new);
	}
	
	public static ZonedDateTime calLocalTimeAfterSpan(ZonedDateTime local, TemporalAmount offset, String remoteZoneId) {
		return ZonedDateTime.of(local.plus(offset).toLocalDateTime(), ZoneId.of(remoteZoneId));
	}
	
	public static long calZonedTimeOffset(ZonedDateTime x, ZonedDateTime y) {
		return x.until(y, ChronoUnit.SECONDS);
	}
	
	public static Appointment[] alert(Appointment... appointments) {
		return Stream.of(appointments).filter(Appointment::withinOneHour).toArray(Appointment[]::new);
	}
	
	public static void main(String... args) {
//		LocalDate date = LocalDate.of(2000, 2, 29);
//		System.out.println(date.plusYears(1));
//		System.out.println(date.plusYears(4));
//		
//		LocalDate today = LocalDate.now();
//		today.with(next(w -> w.getDayOfWeek().getValue() < 6));
		
//		Cal.main("2012", "2");
//		System.out.println(getDaysAlive(LocalDate.of(1983,10,01)));
//		listAll13thFridayIn20thCentury();
//		System.out.println(Arrays.toString(getAllOffsets()));
		System.out.println(Arrays.toString(getZones()));
	}
}

interface Appointment {
	public ZonedDateTime when();
	
	default boolean withinOneHour() {
		LocalDateTime local = when().toLocalDateTime();
		LocalDateTime now   = LocalDateTime.now();
		
		return local.isAfter(now) && now.until(local, ChronoUnit.MINUTES) <= 60;
	}
}

interface TimeInterval {
	public LocalTime getStart();
	public LocalTime getEnd();
	
	default boolean overlapped(TimeInterval that) {
		return !(getEnd().isBefore(that.getEnd()) || getStart().isAfter(that.getEnd()));
	}
	
	static boolean overlapped(TimeInterval t1, TimeInterval t2) {
		return t1.overlapped(t2);
	}
}

class Cal {
	public static void main(String... args) {
		if (args.length < 2) throw new IllegalArgumentException("usage: Cal <year> <month>");
		int year = Integer.valueOf(args[0]);
		int month = Integer.valueOf(args[1]);
		
		final LocalDate date = LocalDate.of(year, month, 1);
		final int DAYS = date.lengthOfMonth();
		final int INIT = date.getDayOfWeek().getValue();
		final int ALL  = DAYS + INIT;
		final int ROWS = (ALL % 7 == 0) ? (ALL / 7) : (ALL / 7 + 1);
		
		StringBuilder sb = new StringBuilder();
		int d = 0;
		for (int i = 0; i < ROWS; i++) {
			for (int j = 0; j < 7; j++) {
				if (i == 0 && j < INIT)
					sb.append("   ");
				else if (d >= DAYS)
					sb.append("  ");
				else {
					if (++d < 10) sb.append("  " + d);
					else sb.append(" " + d);
				}
			}
			sb.append("\n");
		}
		
		System.out.println(sb.toString());
	}
}