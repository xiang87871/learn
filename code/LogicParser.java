
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

/**
 * 逻辑解析器，可以解析 && || ！ 等逻辑，单元逻辑支持自定义
 * 单元逻辑: 最基本的逻辑判断，返回true 或 false
 */
public class LogicParser {

    private static final Logger logger = LoggerFactory.getLogger(recommend.algoframe.recaller.LogicParser.class);

    public static void main(String[] args) {
        test("AllTrue", true);
        test("!AllTrue", false);
        test("!!AllTrue", true);
        test("(AllTrue)", true);
        test("((AllTrue))", true);
        test("AllTrue && AllTrue", true);
        test("!AllTrue && !AllTrue", false);
        test("!AllTrue && !AllTrue ", false);
        test("!AllTrue && (!AllTrue)", false);
        test("!AllTrue || !AllTrue", false);
        test("AllTrue || !AllTrue", true);
        test("!AllTrue || AllTrue", true);
        test("AllTrue || AllTrue", true);
        test(" AllTrue || AllTrue  ", true);
        test(" !AllTrue  ", false);
        test(" !(AllTrue && !AllTrue)", true);
        test(" (!AllTrue && !AllTrue)", false);
        test(" !(AllTrue && !AllTrue) && (AllTrue && !AllTrue)", false);
        test(" !(AllTrue && !AllTrue) && (AllTrue && !AllTrue) || AllTrue", true);
        test("(!(AllTrue && !AllTrue) && (AllTrue && !AllTrue))", false);
        test("(!(!AllTrue && (!AllTrue)) && (AllTrue && !AllTrue))", false);
        test("(!(AllTrue && (!AllTrue)) && !(AllTrue && !AllTrue))", true);
    }

    public enum Relation {
        OR, AND, BRACKETS, NEGATE
    }

    public static class Logic {

        public int start;

        public int end;

        public String current;

        public Relation relateChild;

        public Logic childLogic;

        public Relation relateBro;

        public Logic broLogic;

        public Logic parent;

        public Logic preBro;

        public Predicate<String> predicate;

        public boolean doPredict() {
            try {
                boolean currentPredict = false;
                if (!StringUtils.isEmpty(current)) {
                    currentPredict = predicate.test(current);
                } else if (childLogic != null && relateChild != null) {
                    if (relateChild == Relation.BRACKETS) {
                        currentPredict = childLogic.doPredict();
                    } else {
                        currentPredict = !childLogic.doPredict();
                    }
                }
                if (broLogic != null && relateBro != null) {
                    if (relateBro == Relation.OR) {
                        currentPredict = currentPredict || broLogic.doPredict();
                    } else {
                        currentPredict = currentPredict && broLogic.doPredict();
                    }
                }
                return currentPredict;
            } catch (Exception e) {
                logger.error("do predict fail:", e);
            }
            return false;

        }

        public String buildStr() {
            StringBuilder sb = new StringBuilder();
            if (!StringUtils.isEmpty(current)) {
                sb.append(current);
            } else if (childLogic != null && relateChild != null) {
                if (relateChild == Relation.BRACKETS) {
                    sb.append("(").append(childLogic.buildStr()).append(")");
                } else {
                    sb.append("!").append(childLogic.buildStr());
                }
            }
            if (broLogic != null && relateBro != null) {
                if (relateBro == Relation.OR) {
                    sb.append("||").append(broLogic.buildStr());
                } else {
                    sb.append("&&").append(broLogic.buildStr());
                }
            }
            return sb.toString();
        }

        public int getBroEnd() {
            if (broLogic != null) {
                return broLogic.getBroEnd();
            } else {
                return end;
            }
        }

        public int getChildEnd() {
            return end;
        }

        public Logic(Predicate<String> predicate) {
            this.predicate = predicate;
        }
    }


    public Logic parse(String originStr, Predicate<String> predicate) {
        Logic currenLogic = new Logic(predicate);
        this.parse(currenLogic, originStr.toCharArray(), 0);
        return currenLogic;
    }

    public void parse(Logic currenLogic, char[] chars, int start) {
        int i = start;
        StringBuilder sb = new StringBuilder();
        for (; i < chars.length; i++) {
            if (currenLogic.parent != null && currenLogic.parent.relateChild == Relation.NEGATE) {
                // 跟父的关系是! 遇到空格 && || 要结束
                if (isNegateStop(chars[i])) {
                    currenLogic.end = i - 1;
                    currenLogic.current = sb.toString();
                    return;
                }
            }
            if (isEndBracketsRelation(chars[i])) {
                // 遇到右括号） 结束(开始的子逻辑
                currenLogic.end = i;
                currenLogic.current = sb.toString();
                return;
            }
            if (Character.isWhitespace(chars[i])) {
                continue;
            } else if (isStartRelation(chars[i])) {
                // 开始子逻辑 包括 ! (
                currenLogic.relateChild = chars[i] == '!' ? Relation.NEGATE : Relation.BRACKETS;
                currenLogic.childLogic = new Logic(currenLogic.predicate);
                currenLogic.childLogic.parent = currenLogic;
                parse(currenLogic.childLogic, chars, i + 1);
                // 获取子逻辑的边界
                currenLogic.end = currenLogic.childLogic.getBroEnd();
                i = currenLogic.end;

                if (currenLogic.parent != null && currenLogic.parent.relateChild == Relation.NEGATE) {
                    // 跟父的关系是! 遇到)也要结束
                    if (isEndBracketsRelation(chars[i])) {
                        currenLogic.end = i;
                        currenLogic.current = sb.toString();
                        return;
                    }
                }

            } else if (isBroRelation(chars[i])) {
                // 开始兄弟逻辑，兄弟逻辑的边界就是)，遇到右括号结束兄弟逻辑
                currenLogic.relateBro = chars[i] == '&' ? Relation.AND : Relation.OR;
                currenLogic.current = sb.toString();
                currenLogic.broLogic = new Logic(currenLogic.predicate);
                currenLogic.broLogic.preBro = currenLogic;
                parse(currenLogic.broLogic, chars, i + 2);
                currenLogic.end = currenLogic.getBroEnd();
                i = currenLogic.end;
                // 右括号结束兄弟逻辑
                if (isEndBracketsRelation(chars[currenLogic.end])) {
                    return;
                }
            } else {
                sb.append(chars[i]);
            }
        }
        currenLogic.current = sb.toString();
        currenLogic.end = i - 1;
    }

    private boolean isStartRelation(char chr) {
        return chr == '(' || chr == '!';
    }

    private boolean isBroRelation(char chr) {
        return chr == '|' || chr == '&';
    }

    private boolean isEndBracketsRelation(char chr) {
        return chr == ')';
    }

    private boolean isNegateStop(char chr) {
        return Character.isWhitespace(chr) || chr == '&' || chr == '|' || chr == ')';
    }

    public static void test(String str, boolean except) {
        LogicParser parser = new LogicParser();
        // Logic parse = parser.parse(str, s -> versionCompare("1.11.1", "1.10.1") > 0);
        Logic parse = parser.parse(str, s -> "AllTrue".equals(s));
        String buildStr = parse.buildStr();
        boolean predict = false;
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            predict = parse.doPredict();
        }
        System.out.println("耗时：" + (System.currentTimeMillis() - start));

        System.out.println(String.format("origin:%s lijie:%s except:%s indeed:%s pass:%s", str, buildStr, except, predict, except == predict));
    }

    public static int versionCompare(String version1, String version2) {
        if (version1.equals(version2)) {
            return 0;
        }
        String[] split1 = version1.split("\\.");
        String[] split2 = version2.split("\\.");
        Integer a1 = Integer.parseInt(split1[0]);
        Integer b1 = Integer.parseInt(split2[0]);
        int i = a1.compareTo(b1);
        if (i != 0) {
            return i;
        } else {
            Integer a2 = Integer.parseInt(split1[1]);
            Integer b2 = Integer.parseInt(split2[1]);
            i = a2.compareTo(b2);
            if (i != 0) {
                return i;
            } else {
                Integer a3 = Integer.parseInt(split1[2]);
                Integer b3 = Integer.parseInt(split2[2]);
                i = a3.compareTo(b3);
                return i;
            }
        }
    }

}
