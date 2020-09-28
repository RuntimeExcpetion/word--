package top.mayiweiguishi.practice_online.server.util;

import top.mayiweiguishi.practice_online.server.model.Option;
import top.mayiweiguishi.practice_online.server.model.Question;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将解析后的
 *
 * @author mayiweiguishi
 */
public class WordToJSON2 {
    /**
     * 试题序号
     */
    private final static Pattern NUMBER = Pattern.compile("[\\d+]\\. ");
    /**
     * 试题选项
     */
    private final static Pattern OPTIONS = Pattern.compile("[a-zA-Z]\\. ");

    private final static Pattern SUB_ITEM = Pattern.compile("\\(\\d+\\) ");

    private final static Pattern SCORE = Pattern.compile("<分数>");

    private final static Pattern ANSWER = Pattern.compile("<答案>");

    private final static Pattern ANALYZE = Pattern.compile("<解析>");

    /**
     * 题型
     */
    private final static Pattern TYPE_START = Pattern.compile("(<单选题>)|(<多选题>)|(<填空题>)|(<判断题>)|(<简答题>)");

    private final static Pattern TYPE_END = Pattern.compile("(</单选题>)|(</多选题>)|(</填空题>)|(</判断题>)|(</简答题>)");

    /**
     * 　题目内容集合
     */
    private ArrayList<Question> questions = new ArrayList<>(16);

    private Question question;

    /**
     * 标题/题目位置
     */
    private ArrayDeque<Integer> topics = new ArrayDeque();

    /**
     * 选项位置
     */
    private ArrayDeque<Integer> options = new ArrayDeque();
    
    /**
     * 默认分数
     */
    private final static String DEFAULT_SOCRE = "5";

    /**
     * 返回解析后生成的试卷集合
     *
     * @return
     */
    public ArrayList<Question> getQuestions() {
        return questions;
    }

    /**
     * 该方法将解析后的字符串，进一步解析为试卷集合，为插入到数据库做转换
     *
     * @param word
     */
    public void word2JSON(String word) {
        doParseType(word);
    }

    // 根据试题类型选用不同解析规则
    private void doParseType(String word) {
        Matcher matcherStart = TYPE_START.matcher(word);
        Matcher matcherEnd = TYPE_END.matcher(word);
        int start;
        int end;
        short type;
        while (matcherStart.find() && matcherEnd.find()) {
            for (int i = 0; i < matcherStart.groupCount(); i++) {
                if ((start = matcherStart.end(i + 1)) != -1) {
                    if ((end = matcherEnd.start(i + 1)) == -1) {
                        continue;
                    }
                    type = (short) i;
                    doParseQuestion(word.substring(start, end), type);
                }
            }
        }
    }

    private void doParseQuestion(String text, short type) {
        Matcher numberMatcher = NUMBER.matcher(text);
        while (numberMatcher.find()) {
            topics.addLast(numberMatcher.start());
        }
        if (topics.isEmpty()) {
            return;
        }
        int index = topics.peekFirst();
        String score = null;
        if (index != 0) {
            score = doParseScore(text.substring(0, index));
        }
        while (!topics.isEmpty()) {
            if (topics.size() != 1) {
                doParseTopic(text.substring(topics.pollFirst(), topics.peekFirst()), type, score);
                continue;
            }
            doParseTopic(text.substring(topics.pop()), type, score);
        }
    }

    private void doParseTopic(String text, short type, String score) {
        question = new Question();
        question.setType(type);
        int optionsStart;
        switch (type) {
            case 0:
            case 1:
                optionsStart = doParseOptions(text);
                break;
            case 2:
            case 4:
                optionsStart = doParseSubItem(text, score);
                break;
            default:
                optionsStart = doParseTrueFalseQuestionAnswer(text);
                break;
        }
        StringBuilder sb = new StringBuilder(text.substring(0, optionsStart).replaceFirst("[\\d+]\\. ", ""));
        sb.insert(0, "<p>");
        sb.append("</p>");
        question.setTopic(sb.toString());
        if (question.getScore() == null) {
            question.setScore(Short.valueOf(score));
        }
        questions.add(question);
    }

    private String doParseScore(String text) {
        Matcher parseMatcher = SCORE.matcher(text);
        int index = 0;
        if (parseMatcher.find()) {
            index = parseMatcher.end();
        }
        return index == 0 ? DEFAULT_SOCRE : text.substring(index).trim();
    }

    private int doParseOptions(String text) {
        Matcher optionMatcher = OPTIONS.matcher(text);
        ArrayDeque<String> arrayDeque = new ArrayDeque<>(4);
        while (optionMatcher.find()) {
            int start = optionMatcher.start();
            options.addLast(start);
            arrayDeque.addLast(text.substring(start, start + 1));
        }
        if (options.isEmpty()) {
            return text.length();
        }
        int temp = options.peekFirst();
        List<Option> contents = new ArrayList<>();
        Option option;
        while (!options.isEmpty()) {
            option = new Option();
            option.setNumber(arrayDeque.pollFirst());
            if (options.size() != 1) {
                StringBuilder sb = new StringBuilder(text.substring(options.pollFirst(), options.peekFirst()).replaceFirst("[a-zA-z]\\. ", ""));
                sb.insert(0, "<p>");
                sb.append("</p>");
                option.setContent(sb.toString());
            } else {
                int index = options.pollFirst();
                int end = doParseMultipleAnswer(text.substring(index));
                StringBuilder sb = new StringBuilder(text.substring(index, index + end).replaceFirst("[a-zA-z]\\. ", ""));
                sb.insert(0, "<p>");
                sb.append("</p>");
                option.setContent(sb.toString());
            }
            contents.add(option);
        }
        question.setOptions(contents);
        return temp;
    }

    private int doParseMultipleAnswer(String text) {
        Matcher answerMatcher = ANSWER.matcher(text);
        StringBuilder sb = new StringBuilder(14);
        sb.append("[");
        int end = text.length();
        if (answerMatcher.find()) {
            end = answerMatcher.start();
            int index = doParseAnalyze(text);
            for (int i = answerMatcher.end(); i < index; i++) {
                sb.append("\"");
                sb.append(text.charAt(i));
                sb.append("\"");
            }
        }
        sb.append("]");
        question.setAnswer(sb.toString());
        return end;
    }

    private int doParseBlankFillingAnswer(String text, String score) {
        Matcher answerMatcher = OPTIONS.matcher(text);
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<>();
        ArrayDeque<String> number = new ArrayDeque<>();
        while (answerMatcher.find()) {
            arrayDeque.addLast(answerMatcher.start());
            number.addLast(text.substring(answerMatcher.start(), answerMatcher.end() - 2));
        }
        if (arrayDeque.isEmpty()) {
            return doParseAnalyze(text);
        }
        question.setScore((short) (arrayDeque.size() * Short.valueOf(score)));
        ArrayList<String> arrayList = new ArrayList<>(arrayDeque.size());
        Option option;
        int end = arrayDeque.peekFirst();
        while (!arrayDeque.isEmpty()) {
            option = new Option();
            option.setNumber(number.pollFirst());
            if (arrayDeque.size() != 1) {
                StringBuilder sb = new StringBuilder(text.substring(arrayDeque.pollFirst(), arrayDeque.peekFirst())
                        .replaceFirst("[a-zA-Z]\\. ", "").trim());
                sb.insert(0, "<p>");
                sb.append("</p>\n");
                option.setContent(sb.toString());
            } else {
                int index = doParseAnalyze(text);
                StringBuilder sb = new StringBuilder(text.substring(arrayDeque.pollFirst(), index)
                        .replaceFirst("[a-zA-Z]\\. ", "").trim());
                sb.insert(0, "<p>");
                sb.append("</p>\n");
                option.setContent(sb.toString());
            }
            arrayList.add(option.toString());
        }
        question.setAnswer(arrayList.toString());
        return end;
    }

    private int doParseTrueFalseQuestionAnswer(String text) {
        Matcher answerMatcher = ANSWER.matcher(text);
        StringBuilder sb = new StringBuilder(14);
        sb.append("[");
        int end = text.length();
        if (answerMatcher.find()) {
            end = answerMatcher.start();
            int index = doParseAnalyze(text);
            for (int i = answerMatcher.end(); i < index; i++) {
                sb.append("\"");
                sb.append(text.charAt(i));
                sb.append("\"");
            }
        }
        sb.append("]");
        question.setAnswer(sb.toString());
        return end;
    }

    private int doParseSubItem(String text, String score) {
        Matcher subItem = SUB_ITEM.matcher(text);
        ArrayDeque<Integer> arrayDeque = new ArrayDeque<>();
        ArrayDeque<String> number = new ArrayDeque<>();
        int start;
        int end;
        while (subItem.find()) {
            start = subItem.start();
            end = subItem.end();
            arrayDeque.addLast(start);
            number.addLast(text.substring(start + 1, end - 2));
        }
        if (arrayDeque.isEmpty()) {
            return doParseBlankFillingAnswer(text, score) - 4;
        }
        end = arrayDeque.peekFirst();
        List<Option> contents = new ArrayList<>();
        Option option;
        while (!arrayDeque.isEmpty()) {
            option = new Option();
            option.setNumber(number.pollFirst());
            if (arrayDeque.size() != 1) {
                option.setContent(text.substring(arrayDeque.pollFirst(), arrayDeque.peekFirst()).replaceFirst("\\(\\d+\\) ", ""));
            } else {
                int index = arrayDeque.pollFirst();
                int offset = doParseBlankFillingAnswer(text.substring(index), score);
                option.setContent(text.substring(index, index + offset).replaceFirst("\\(\\d+\\) ", ""));
            }
            contents.add(option);
        }
        question.setOptions(contents);
        return end;
    }

    private int doParseAnalyze(String text) {
        Matcher analyzeMatcher = ANALYZE.matcher(text);
        int end = text.length();
        if (analyzeMatcher.find()) {
            end = analyzeMatcher.start();
            question.setAnalyze(text.substring(analyzeMatcher.end()));
        }
        return end;
    }

}
