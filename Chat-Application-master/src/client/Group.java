package client;

import java.util.Comparator;

public class Group implements Comparator<Group> {
    private final int id;
    private final String name;

    public Group(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    @Override
    public int compare(Group o1, Group o2) {
        return name.compareTo(o2.getName());
    }
}
