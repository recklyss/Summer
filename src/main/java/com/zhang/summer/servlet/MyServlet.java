package com.zhang.summer.servlet;

import com.zhang.summer.MyAnnotation.MyAutowrite;
import com.zhang.summer.MyAnnotation.MyController;
import com.zhang.summer.MyAnnotation.MyRequestMapping;
import com.zhang.summer.MyAnnotation.MyService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author zhangjiaheng
 * @Description
 **/
public class MyServlet extends HttpServlet {

    private List<String> classNames = new ArrayList<String>();

    /**
     * IOC容器保存bean
     */
    private Map<String, Object> iocMap = new ConcurrentHashMap<String, Object>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    public void init() throws ServletException {
        // 1、Tomcat启动过程中，加载war包时，首先扫描war包下面的类声明的spring指定的注解
        try {
            basePackageScan("com.zhang");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 2、扫描找到对应的类
        // 3、并创建对象（Spring中使用反射创建对象class.forName方法）
        try {
            doInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 4、创建自己的IOC容器

        // 5、将创建好的对象放到自己定义的IOC容器中
        // 6、@Autowrite注解到的对象会在IOC容器中get对应的对象，注入到指定的对象中
        doAutowrite();
        // 7、@RequestMapping上的路径如何映射到对应的控制类和方法上？：：：启动时将控制类中所有路径映射到方法上，绑定起来
        // urlMapping();

    }

    private void doAutowrite() {
        // 遍历容器中的所有bean
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();
            if (clazz.isAnnotationPresent(MyController.class)) {
                // bean是控制类
                // 拿到所有成员变量方法 拿到声明了autowrite注解的成员变量
                Field[] fields = clazz.getFields();
                for (Field field : fields) {
                    // 判断哪些需要注入
                    if(field.isAnnotationPresent(MyAutowrite.class)){
                        MyAutowrite autowrite = field.getAnnotation(MyAutowrite.class);
                        String key = autowrite.value();
                        Object needAutowrite = iocMap.get(key);

                    }
                }
            }
        }
    }

    /**
     * 实例化对象
     */
    private void doInstance() throws Exception {

        for (String className : classNames) {
            String cn = className.replace(".class", "");
            // class对象 通过反射拿到注解等信息
            Class<?> clazz = Class.forName(cn);
            if (clazz.isAnnotationPresent(MyController.class)) {
                // 扫描到控制层的类 需要实例化
                Object instance = clazz.newInstance(); // map.put("<bean名称>", instance);
                String requestMappingValue = clazz.getAnnotation(MyRequestMapping.class).value();
                // 向容器中加入bean
                iocMap.put(requestMappingValue, instance);
            } else if (clazz.isAnnotationPresent(MyService.class)) {
                Object instance = clazz.newInstance(); // map.put("<bean名称>", instance);
                String requestMappingValue = clazz.getAnnotation(MyService.class).value();
                // 向容器中加入bean
                iocMap.put(requestMappingValue, instance);
            }

        }


    }

    private void basePackageScan(String packageScan) throws IOException {
        URL path = this.getClass().getClassLoader().getResources("/" + packageScan.replaceAll("\\.", "\\")).nextElement();
        String fileStr = path.getFile();
        File file = new File(fileStr);
        String[] fileStrs = file.list();
        // 拿出来扫描的所有文件和文件夹 筛选出所有的文件
        for (String filePath : fileStrs) {
            File thisFile = new File(filePath);
            if (thisFile.isDirectory()) {
                basePackageScan(packageScan + "." + filePath);
            } else {
                // 扫描到class，保存
                String thisClassName = packageScan + "." + thisFile.getName();
                classNames.add(thisClassName);
            }
        }
    }
}
