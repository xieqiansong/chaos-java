package lan.chaos.tools;

public class TransformUtil {

    private static final double earthR = 6378137.0;
    private static final double ee = 0.00669342162296594323;

    public static boolean outOfChina(double lat, double lng) {
        if ((lng < 72.004) || (lng > 137.8347)) {
            return true;
        }
        if ((lat < 0.8293) || (lat > 55.8271)) {
            return true;
        }
        return false;
    }

    public static double transformLat(double x, double y) {
        double ret =
                -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    public static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    public static double[] transform(double x, double y) {
        double xy = x * y;
        double absX = Math.sqrt(Math.abs(x));
        double xPi = x * Math.PI;
        double yPi = y * Math.PI;
        double d = 20.0 * Math.sin(6.0 * xPi) + 20.0 * Math.sin(2.0 * xPi);

        double lat = d;
        double lng = d;

        lat += 20.0 * Math.sin(yPi) + 40.0 * Math.sin(yPi / 3.0);
        lng += 20.0 * Math.sin(xPi) + 40.0 * Math.sin(xPi / 3.0);

        lat += 160.0 * Math.sin(yPi / 12.0) + 320 * Math.sin(yPi / 30.0);
        lng += 150.0 * Math.sin(xPi / 12.0) + 300.0 * Math.sin(xPi / 30.0);

        lat *= 2.0 / 3.0;
        lng *= 2.0 / 3.0;

        lat += -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * xy + 0.2 * absX;
        lng += 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * xy + 0.1 * absX;

        return new double[]{lat, lng};
    }

    public static double[] delta(double lat, double lng) {
        double[] delta = new double[2];
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLon(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        delta[0] = (dLat * 180.0) / ((earthR * (1 - ee)) / (magic * sqrtMagic) * Math.PI);
        delta[1] = (dLng * 180.0) / (earthR / sqrtMagic * Math.cos(radLat) * Math.PI);
        return delta;
    }

    public static double[] deltaLngLat(double lat, double lng) {
        double[] d = transform(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        double[] delta = new double[2];
        delta[0] = (d[0] * 180.0) / ((earthR * (1 - ee)) / (magic * sqrtMagic) * Math.PI);
        delta[1] = (d[1] * 180.0) / (earthR / sqrtMagic * Math.cos(radLat) * Math.PI);
        return delta;
    }

    public static double[] wgs2gcj(double wgsLat, double wgsLng) {
        if (outOfChina(wgsLat, wgsLng)) {
            return new double[]{wgsLat, wgsLng};
        }
        double[] d = deltaLngLat(wgsLat, wgsLng);
        return new double[]{wgsLat + d[0], wgsLng + d[1]};
    }

    public static double[] gcj2wgs(double gcjLat, double gcjLng) {
        if (outOfChina(gcjLat, gcjLng)) {
            return new double[]{gcjLat, gcjLng};
        }

        double wgsLat = gcjLat;
        double wgsLng = gcjLng;
        double threshold = 1e-7;  // 收敛阈值
        int maxIterations = 30;   // 最大迭代次数

        for (int i = 0; i < maxIterations; i++) {
            double[] delta = deltaLngLat(wgsLat, wgsLng);
            double prevLat = wgsLat;
            double prevLng = wgsLng;

            // 通过正向加密算法计算当前猜测值的偏移量
            double[] currentGcj = wgs2gcj(wgsLat, wgsLng);
            double dLat = currentGcj[0] - gcjLat;
            double dLng = currentGcj[1] - gcjLng;

            // 调整猜测值
            wgsLat -= dLat;
            wgsLng -= dLng;

            // 判断收敛条件
            if (Math.abs(wgsLat - prevLat) < threshold && Math.abs(wgsLng - prevLng) < threshold) {
                break;
            }
        }
        return new double[]{wgsLat, wgsLng};
    }
}
