package com.github.xiang87871.tree;

import lombok.Data;
import org.omg.CORBA.PUBLIC_MEMBER;

/**
 * @author ：laizhixiang
 * @date ：Created in 2022/6/8 20:53
 * @modified By：
 * @version: 0.0.1
 */
@Data
public class AVLTree {

    /**
     * 根节点
     */
    private Node root;


    @Data
    public static class Node {
        public Node(int data) {
            this.data = data;
        }

        /**
         * 结点数据
         */
        private int data;
        /**
         * 左节点
         */
        private Node left;

        /**
         * 右结点
         */
        private Node right;

        /**
         * 高 叶子节点高度是1
         */
        private int height=1;

        public String forward() {
            StringBuilder sb = new StringBuilder();
            if(left != null) {
                sb.append(left.forward()).append(",");
            }
            sb.append(data).append(",");
            if(right != null) {
                sb.append(right.forward()).append(",");
            }
            return sb.subSequence(0, sb.length()-1).toString();
        }


        private String numPadingStr(int num) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < num; i++) {
                sb.append("\t");
            }
            return sb.toString();
        }
    }

    public int getHeight() {
        if(root == null) {
            return 0;
        }
        return root.height;
    }

    /**
     * 获取某个节点的高度
     *
     * @param node
     * @return
     */
    public int computeNodeHeight(Node node) {
        throwIf(node == null, "node 不能为空");
        return node.height = Math.max(node.getLeft() != null ? node.getLeft().getHeight() : 0, node.getRight() != null ? node.getRight().getHeight() : 0) + 1;
    }

    public int getHeight(Node node) {
        throwIf(node == null, "node 不能为空");
        return Math.max(node.getLeft() != null ? node.getLeft().getHeight() : 0, node.getRight() != null ? node.getRight().getHeight() : 0) + 1;
    }

    /**
     * 平衡状态
     *
     * @param node
     * @return 0: 平衡，-1：左长，1：右长
     */
    public int blanceStatus(Node node) {
        int blance = (node.right == null ? 0 : node.right.height) - (node.left == null ? 0 : node.left.height);
        if (blance > 1) {
            return 1;
        } else if (blance >= -1) {
            return 0;
        } else {
            return -1;
        }
    }

    public void print() {
        System.out.println(root.forward());
    }

    /**
     * 插入数据
     *
     * @param data
     */
    public void insert(int data) {
        root = insert(root, data);
    }

    /**
     * 插入数据
     *
     * @param node
     * @param data
     */
    public Node insert(Node node, int data) {
        if (node == null) {
            return new Node(data);
        }
        if (node.data == data) {
            return node;
        }

        if (data < node.getData()) {
            node.left = insert(node.left, data);
            computeNodeHeight(node);
        } else {
            node.right = insert(node.right, data);
            computeNodeHeight(node);
        }
        int i = blanceStatus(node);
        if (i == -1) {
            // 左高
            if(node.left.data < data) {
                node.left = leftRotate(node.left);
            }
            node = rightRotate(node);
//            if (node.left.right != null) {
//                //左括号( 类型，先转成/型
//                node.left = leftRotate(node.left);
//                node = rightRotate(node);
//            } else if (node.left.left != null) {
//                // /型
//                node = rightRotate(node);
//            }
        } else if (i == 1) {
            // 右高
            if(node.right.data > data) {
                node.right = rightRotate(node.right);
            }
            node = leftRotate(node);
//            if (node.right.left != null) {
//                node.right = rightRotate(node.right);
//                node = leftRotate(node);
//            } else if (node.right.right != null) {
//                node = leftRotate(node);
//            }
        }
        return node;
    }

    /**
     * 左旋
     *
     * @param node
     * @return
     */
    public Node leftRotate(Node node) {
        throwIf(node == null || node.right == null, "节点不满足左转条件");
        Node mid = node.right;
        node.right = mid.left;
        mid.left = node;
        computeNodeHeight(mid.left);
        computeNodeHeight(mid);
        return mid;
    }


    /**
     * 右旋
     *
     * @param node
     * @return
     */
    public Node rightRotate(Node node) {
        throwIf(node == null || node.left == null, "节点不满足左转条件");
        Node mid = node.left;
        node.left = mid.right;
        mid.right = node;
        computeNodeHeight(mid.right);
        computeNodeHeight(mid);
        return mid;
    }

    public void throwIf(boolean flag, String error) {
        if (flag) {
            throw new RuntimeException(error);
        }
    }

    public static void main(String[] args) {
        AVLTree avlTree = new AVLTree();
        int[] datas = new int[]{0,10,5,2,1,6,33};
//        int[] datas = new int[]{0,1,2,5,6,10,33};

        for (int data : datas) {
            avlTree.insert(data);
        }
        avlTree.print();
        System.out.println(avlTree.getHeight());
    }
}
