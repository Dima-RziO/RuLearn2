package vladis.luv.rulearn;

import static ru.dimarzio.rulearn2.utils.PrimitivesKt.normalized;

public class Utils {
    public static String getHint(String txtInput, String word) {
        String input = normalizeString(txtInput);
        String nword = normalizeString(word);

        String correct_substr = "";

        String answer = "";

        for (int i = input.length(); i > 0; i--) {
            //переменные только для наглядности
            if (i <= nword.length()) { //подпорка, тк в поле ввода может быть текст длиннее, чем в nsgtring
                String input_substr = input.substring(0, i);
                String nword_substr = nword.substring(0, i);
                if (input_substr.equals(nword_substr)) {
                    correct_substr = input_substr;
                    break;
                }
            }
        }

        //System.out.println("correct substr is : " + correct_substr);
        //
        //correct_substr содержит правильную часть нормализованного слова
        //пока часть нормализованного word совпадает с correct_substr, копируем часть word в ответ
        //
        for (int i = 0; i <= word.length(); i++) {
            answer = word.substring(0, i);
            if (correct_substr.equals(normalizeString(answer))) {
                //мы нашли основу, добавляем харакаты, пока основа снова станет не равной или не кончится слово
                for (int j = i; j <= word.length(); j++) {
                    answer = word.substring(0, j);
                    if (!correct_substr.equals(normalizeString(answer))) {
                        break;
                    }
                }

                break;
            }
        }
        return answer;
    }

    private static String normalizeString(String txt) {
        return normalized(txt);
    }
}
