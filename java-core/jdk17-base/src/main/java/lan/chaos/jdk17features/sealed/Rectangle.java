package lan.chaos.jdk17features.sealed;

/** 矩形：final record 实现密封接口。 */
public record Rectangle(double w, double h) implements Shape {
    @Override
    public double area() {
        return w * h;
    }
}
