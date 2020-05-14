import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Solution {
    public static void main (String[] args) throws IOException {
        new HashMap();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line = br.readLine();
        if(line==null||line.equals("")){
            System.out.println("null");
            return;
        }
        int num = Integer.parseInt(line);
        int result=funcation(num);
        System.out.println(result);
    }
    public static int funcation(Integer n){
        List<Integer> m = new ArrayList<>();
        if(n<10) {
            return n;
        }
        while(n>=10){
            for (int i = 9; i >= 2; i--) {
                if (0 == n % i){
                    m.add(i);
                    n = n / i;
                    break;
                }
                if (2 == i) return -1;
            }
        }
        m.add(n);
        int minValue = 0;
        System.out.println(m);
        for (int i = m.size()-1; i >=0 ; i--) {
            minValue = minValue*10+m.get(i);
        }
        return minValue;
    }
}