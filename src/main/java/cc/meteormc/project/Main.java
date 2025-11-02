package cc.meteormc.project;

import cc.meteormc.project.target.TestClass;

public class Main {
    public static void main(String[] args) {
        TestClass.test();
        TestClass test = new TestClass();
        test.print();
        System.out.println(test.resolve(2, 3));
    }
}
