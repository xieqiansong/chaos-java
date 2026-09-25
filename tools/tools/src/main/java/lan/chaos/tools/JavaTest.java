package lan.chaos.tools;

import cn.hutool.core.lang.Console;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.primitive.IntLists;

/**
 * 快速测试
 */
public class JavaTest {


    public static void main(String[] args) throws Exception {

        Console.log(Lists.mutable.with("a", "b", "c"));
        Console.log(IntLists.mutable.with(1, 2, 3));

    }


}
