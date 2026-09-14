package lan.chaos.jdk8features.common.model;

import java.time.LocalDate;

/**
 * 样例数据模型：贯穿各特性演示（Stream 分组、Optional 取值、方法引用等）。
 * 自带 {@link #sample()} 工厂，调用方无需自己准备输入。
 */
public class User {

    private String name;
    private int age;
    private String city;
    private LocalDate birthday;

    public User() {
    }

    public User(String name, int age, String city, LocalDate birthday) {
        this.name = name;
        this.age = age;
        this.city = city;
        this.birthday = birthday;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    /** 样例工厂：默认一个用户，方便演示 */
    public static User sample() {
        return new User("张三", 28, "北京", LocalDate.of(1996, 5, 1));
    }

    @Override
    public String toString() {
        return name + "(" + age + "," + city + ")";
    }
}
