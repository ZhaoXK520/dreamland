public class test implements Runnable{
    private boolean flag = false;

    test(boolean b){
        flag = b;
    }

    protected void finalize(){
        if(flag==true)
        System.out.println("回收");
    }

    @Override
    public void run() {
        while (true){

        }
    }
}
