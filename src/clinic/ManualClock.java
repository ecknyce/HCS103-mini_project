package clinic;

public class ManualClock implements Clock {
    private int currentHour;

    public ManualClock(int currentHour) { setNow(currentHour); }
    @Override public int now() { return currentHour; }

    public void setNow(int hour) {
        if (hour < 0 || hour > 168) throw new IllegalArgumentException("Clock hour must be from 0 to 168.");
        currentHour = hour;
    }
}
