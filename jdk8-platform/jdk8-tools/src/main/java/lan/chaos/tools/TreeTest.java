package lan.chaos.tools;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import lan.chaos.model.OrganizationEntity;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class TreeTest {

    /**
     * 后序遍历树结构
     */
    public static void postOrderTraversal(Tree<String> root, Consumer<Tree<String>> visit) {
        if (CollUtil.isNotEmpty(root.getChildren())) {
            for (Tree<String> child : root.getChildren()) {
                postOrderTraversal(child, visit);
            }
        }
        visit.accept(root);
    }

    /**
     * 前序遍历树结构
     *
     * @param visit 返回值表示是否继续遍历子节点
     */
    public static void preOrderTraversal(Tree<String> root, Function<Tree<String>, Boolean> visit) {
        Boolean apply = visit.apply(root);
        if (apply && CollUtil.isNotEmpty(root.getChildren())) {
            for (Tree<String> child : root.getChildren()) {
                preOrderTraversal(child, visit);
            }
        }
    }

    public void testTree() throws Exception {
        List<OrganizationEntity> orgMpEntities = Db.use().findAll(Entity.create("sys_org").set("delete_flag", "1"))
                .stream().map(o -> BeanUtil.copyProperties(o, OrganizationEntity.class)).toList();
        //配置
        TreeNodeConfig treeNodeConfig = new TreeNodeConfig();
        // 自定义属性名 都有默认值的
        treeNodeConfig.setWeightKey("sortCode");
        treeNodeConfig.setNameKey("orgName");

        //转换器 (含义:找出父节点为字符串零的所有子节点, 并递归查找对应的子节点, 深度最多为 3)
        List<Tree<String>> treeNodes = TreeUtil.build(orgMpEntities, "0", treeNodeConfig,
                (treeNode, tree) -> {
                    tree.setId(treeNode.getId());
                    tree.setParentId(treeNode.getParentId());
                    tree.setWeight(Optional.ofNullable(treeNode.getSortCode()).orElse(0));
                    tree.setName(treeNode.getOrgName());
                    // 扩展属性 ...
                    tree.putExtra("orgCode", treeNode.getOrgCode());
                });
        Console.log();
    }
}
