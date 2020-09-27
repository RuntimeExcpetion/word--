//package top.mayiweiguishi.practice_online.server.util;
//
//import top.mayiweiguishi.practice_online.server.model.Option;
//import top.mayiweiguishi.practice_online.server.model.Question;
//
//import java.util.ArrayDeque;
//import java.util.ArrayList;
//import java.util.TreeMap;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * 将解析后的
// *
// * @author root
// */
//public class WordToJSON {
//    /**
//     * 试卷序号
//     */
//    private final static Pattern NUMBER = Pattern
//            .compile("([一|二|三|四|五|六|七|八|九|十]+[\\.])|([a-zA-Z]\\. )|([\\d+]\\. )");
//    /**
//     * 题型
//     */
//    private final static Pattern TYPE = Pattern.compile("(单项选择题)|(多项选择题)|(判断题)|(填空题)|(问答题)|(简答题)|(综合分析题)");
//    /**
//     * 　题目内容集合
//     */
//    private ArrayList<Question> questions = new ArrayList<>(16);
//    /**
//     * 保存题目类型位置，用于进一步给各个题目标识类型
//     */
//    private TreeMap<Integer, Short> TYPE_POSITION = new TreeMap<>();
//
//    /**
//     * 标题/题目位置
//     */
//    private ArrayDeque<Integer> topics = new ArrayDeque();
//    /**
//     * 题目位置
//     */
//    private ArrayDeque<Integer> contents = new ArrayDeque();
//    /**
//     * 选项位置
//     */
//    private ArrayDeque<Integer> options = new ArrayDeque();
//
//    /**
//     * 返回解析后生成的试卷集合
//     *
//     * @return
//     */
//    public ArrayList<Question> getQuestions() {
//        return questions;
//    }
//
//    /**
//     * 该方法将解析后的字符串，进一步解析为试卷集合，为插入到数据库做转换
//     *
//     * @param word
//     */
//    public void word2JSON(String word) {
//        doParseQuestion(word);
//        doParseType(word);
//        doparse2Json(word);
//    }
//
//    /**
//     * 解析试卷内容对应位置
//     *
//     * @param word
//     */
//    private void doParseQuestion(String word) {
//        Matcher numberMatcher = NUMBER.matcher(word);
//        // 将匹配到的位置添加上述集合中
//        while (numberMatcher.find()) {
//            int topic = numberMatcher.start(1);
//            int comment = numberMatcher.start(3);
//            int option = numberMatcher.start(2);
//            if (topic != -1) {
//                topics.add(topic);
//            }
//            if (comment != -1) {
//                contents.add(comment);
//            }
//            if (option != -1) {
//                options.add(option);
//            }
//        }
//    }
//
//    private void doParseType(String word) {
//        Matcher typeMatcher = TYPE.matcher(word);
//        while (typeMatcher.find()) {
//            int one = typeMatcher.start(1);
//            int two = typeMatcher.start(2);
//            int three = typeMatcher.start(3);
//            int four = typeMatcher.start(4);
//            short type = 0;
//            if (one != -1) {
//                type = 1;
//            }
//            if (two != -1) {
//                type = 2;
//            }
//            if (three != -1) {
//                type = 3;
//            }
//            if (four != -1) {
//                type = 4;
//            }
//            TYPE_POSITION.put(typeMatcher.start(), type);
//        }
//    }
//
//    private void doparse2Json(String word) {
//        Question question = null;
//        ArrayList<Option> option = new ArrayList<>();
//        while (!topics.isEmpty() || !contents.isEmpty() || !options.isEmpty()) {
//            // 如果标题不为空
//            if (!topics.isEmpty() && !contents.isEmpty() && topics.peekFirst() < contents.peekFirst()) {
//                topics.pollFirst();
//                continue;
//            }
//            //　如果题目描述不为空
//            if (!contents.isEmpty()) {
//                if (!options.isEmpty() && contents.peekFirst() < options.peekFirst()) {
//                    // 记录开始的位置
//                    int start = contents.pollFirst() + 2;
//                    // 判断选择集合是否有元素
//                    if (option.size() != 0 && question != null) {
//                        question.setOptions(option);
//                        option = new ArrayList<>();
//                    }
//                    // 如果问题类不为空
//                    if (question != null) {
//                        if (TYPE_POSITION.size() != 0) {
//                            Integer key = TYPE_POSITION.lowerKey(start);
//                            question.setType(TYPE_POSITION.get(key));
//                        }
//                        questions.add(question);
//                    }
//                    // 再新建
//                    question = new Question();
//                    // 问题描述
//                    String desc;
//                    int end;
//                    if (!contents.isEmpty() && contents.peekFirst() < options.peekFirst()) {
//                        if (!topics.isEmpty() && contents.peekFirst() > topics.peekFirst()) {
//                            end = topics.peekFirst();
//                        } else {
//                            end = contents.peekFirst();
//                        }
//                        desc = word.substring(start, end);
//                        question.setTopic(desc);
//                        continue;
//                    }
//                    desc = word.substring(start, options.peekFirst());
//                    question.setTopic(desc);
//                    continue;
//                } else if (options.isEmpty()) {
//                    // 记录开始的位置
//                    int start = contents.pollFirst() + 2;
//                    // 判断选择集合是否有元素
//                    if (option.size() != 0) {
//                        question.setOptions(option);
//                        option = new ArrayList<>();
//                    }
//                    // 如果问题类不为空
//                    if (question != null && null != question.getTopic()) {
//                        if (TYPE_POSITION.size() != 0) {
//                            Integer key = TYPE_POSITION.lowerKey(start);
//                            question.setType(TYPE_POSITION.get(key));
//                        }
//                        questions.add(question);
//                    }
//                    // 再新建
//                    question = new Question();
//                    // 问题描述
//                    String desc;
//                    int end;
//                    if (!topics.isEmpty() && !contents.isEmpty() && contents.peekFirst() > topics.peekFirst()) {
//                        end = topics.peekFirst();
//                    } else {
//                        if (contents.isEmpty()) {
//                            if (topics.isEmpty()) {
//                                desc = word.substring(start);
//                            } else {
//                                desc = word.substring(start, topics.peekFirst());
//                            }
//                            question.setTopic(desc);
//                            questions.add(question);
//                            continue;
//                        }
//                        end = contents.peekFirst();
//                    }
//                    desc = word.substring(start, end);
//                    question.setTopic(desc);
//                    continue;
//                }
//            }
//            // 如果选项不为空
//            if (!options.isEmpty()) {
//                // 如果选项集合只有一个元素
//                if (options.size() == 1) {
//                    if (topics.isEmpty()) {
//                        option.add(word.substring(options.pollFirst() + 2));
//                    } else {
//                        int start = options.pollFirst() + 2;
//                        int end = 0;
//                        if (contents.isEmpty()) {
//                            end = topics.peekFirst();
//                        } else if (start < contents.peekFirst()) {
//                            end = contents.peekFirst();
//                        }
//                        String desc = word.substring(start, end);
//                        option.add(desc);
//                    }
//                    if (question != null) {
//                        question.setOptions(option);
//                        questions.add(question);
//                        question = new Question();
//                    }
//                    continue;
//                }
//                int start = options.pollFirst() + 2;
//                if (contents.isEmpty()) {
//                    option.add(word.substring(start, options.peekFirst()));
//                    continue;
//                }
//                if (options.peekFirst() < contents.peekFirst()) {
//                    option.add(word.substring(start, options.peekFirst()));
//                    continue;
//                }
//                option.add(word.substring(start, contents.peekFirst()));
//                continue;
//            }
//            if (topics.size() == 1) {
//                if (question != null) {
//                    questions.add(question);
//                }
//                question = new Question();
//                question.setTopic(word.substring(topics.pollFirst() + 2));
//                questions.add(question);
//                continue;
//            }
//            if (question == null) {
//                question = new Question();
//            }
//            if (!topics.isEmpty()) {
//                int start = topics.pollFirst() + 2;
//                question.setTopic(word.substring(start, topics.peekFirst()));
//                questions.add(question);
//                question = null;
//            }
//        }
//    }
//
//}
