package lan.chaos.jdk17features.sealed;

/** 圆：final 满足 sealed 对 permitted 子类的约束（必须 final / sealed / non-sealed 之一）。 */
public record Circle(double r) implements Shape {
    @Override
    public double area() {
        return Math.PI * r * r;
    }
}
