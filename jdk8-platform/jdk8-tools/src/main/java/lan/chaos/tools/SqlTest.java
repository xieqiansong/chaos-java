package lan.chaos.tools;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;

import java.util.List;

public class SqlTest {
    private static void test_20251005() {
        {
            String sql = "SELECT t1.name, t1.department, derived_table.avg_salary " +
                    "FROM employees t1 " +
                    "INNER JOIN ( " +
                    "    SELECT department, AVG(salary) AS avg_salary " +
                    "    FROM employees " +
                    "    WHERE status = 'full-time' " +
                    "    GROUP BY department " +
                    ") derived_table " +
                    "ON t1.department = derived_table.department " +
                    "WHERE t1.salary > derived_table.avg_salary";

            // 1. 使用 SQLUtils 解析 SQL，获取语句列表
            // 第二个参数是 DbType，用于指定 SQL 的方言（如 mysql, oracle, postgresql）
            List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, DbType.mysql);

            // 通常一条语句对应一个 SELECT
            if (stmtList.isEmpty() || !(stmtList.getFirst() instanceof SQLSelectStatement)) {
                System.out.println("Not a SELECT statement or no statement found.");
                return;
            }
            SQLSelectStatement selectStatement = (SQLSelectStatement) stmtList.getFirst();
            // 2. 创建一个自定义 Visitor 来遍历 AST
            selectStatement.accept(new MyASTVisitor());
        }
        {
            String sql = "SELECT employee_id, name, department, salary " +
                    "FROM employees " +
                    "WHERE department IN (" +
                    "    SELECT department_name " +
                    "    FROM departments " +
                    "    WHERE location = 'Shanghai' AND budget > 1000000" +
                    ") " +
                    "AND salary > 50000";

            // 1. 解析 SQL
            List<SQLStatement> stmtList = SQLUtils.parseStatements(sql, DbType.mysql);

            if (stmtList.isEmpty() || !(stmtList.getFirst() instanceof SQLSelectStatement)) {
                System.out.println("Not a SELECT statement or no statement found.");
                return;
            }
            SQLSelectStatement selectStatement = (SQLSelectStatement) stmtList.getFirst();
            // 2. 使用自定义 Visitor 遍历 AST
            selectStatement.accept(new WhereInVisitor());
        }
    }

    // 自定义 Visitor，继承自 SQLASTVisitorAdapter
    static class MyASTVisitor extends SQLASTVisitorAdapter {

        // 重写 visit 方法，针对我们关心的节点类型
        // 重点：访问派生表（即子查询作为表源）
        @Override
        public boolean visit(SQLSubqueryTableSource x) {
            System.out.println(">>> Found a Derived Table (Subquery in FROM clause)! <<<");
            System.out.println("Its Alias is: " + x.getAlias());

            // 这个派生表的 SQLSelect 对象包含了子查询的全部结构
            System.out.println("--- Start of Subquery AST ---");
            // 格式化输出子查询的 SQL，这本身就是一种分析
            String subQuerySql = SQLUtils.toSQLString(x.getSelect(), DbType.mysql);
            System.out.println("The subquery is: " + subQuerySql);
            System.out.println("--- End of Subquery AST ---\n");

            // 返回 true 表示继续遍历这个子查询内部的节点
            // 返回 false 则会跳过这个子查询内部的遍历
            return true;
        }
    }


    static class WhereInVisitor extends SQLASTVisitorAdapter {

        // 重点：访问 IN 子查询表达式
        @Override
        public boolean visit(SQLInSubQueryExpr x) {
            System.out.println(">>> Found a WHERE IN SubQuery! <<<");

            // 获取 IN 操作符左边的表达式（这里是 department）
            String leftExpr = SQLUtils.toSQLString(x.getExpr(), DbType.mysql);
            System.out.println("Left expression: " + leftExpr);

            // 获取子查询
            System.out.println("--- Start of IN SubQuery ---");
            String subQuerySql = SQLUtils.toSQLString(x.getSubQuery(), DbType.mysql);
            System.out.println("The IN subquery is: " + subQuerySql);
            System.out.println("--- End of IN SubQuery ---\n");

            // 返回 true 表示继续遍历子查询内部的节点
            return true;
        }
    }
}
