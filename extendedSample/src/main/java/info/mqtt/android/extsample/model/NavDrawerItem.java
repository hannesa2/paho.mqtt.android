package info.mqtt.android.extsample.model;


import info.mqtt.android.extsample.activity.Connection;

public class NavDrawerItem {
    private final String title;
    private final String handle;

    public NavDrawerItem(Connection connection) {
        this.title = connection.getId();
        this.handle = connection.handle();
    }

    public String getTitle() {
        return title;
    }

    public String getHandle() {
        return handle;
    }

}
