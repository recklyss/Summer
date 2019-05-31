package com.zhang.summer.servlet;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.zhang.summer.MyAnnotation.MyAutowrite;
import com.zhang.summer.MyAnnotation.MyController;
import com.zhang.summer.MyAnnotation.MyRequestMapping;
import com.zhang.summer.MyAnnotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author zhangjiaheng
 * @Description servlet容器
 **/
public class MyServlet extends HttpServlet {

    private Properties properties = new Properties();

    private static final Log log = LogFactory.get();

    private List<String> classNames = new ArrayList<String>();

    private List<Handler> handlerList = new ArrayList<Handler>();

    /**
     * IOC容器保存bean
     */
    private Map<String, Object> iocMap = new ConcurrentHashMap<String, Object>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("请求到达doGet方法，开始处理------");
        // 分发请求
        doDispatcherServlet(req, resp);
        log.info("doGet方法，结束处理------");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            // 0、加载配置文件 首先读取配置在web.xm中的servlet参数
            doLoadProperties(config.getInitParameter("contextConfigLocation"));
            // 1、Tomcat启动过程中，加载war包时，首先根据配置或者指定的包扫描war包下面的类声明的spring指定的注解
            doBasePackageScan(properties.getProperty("scanPackage"));
            // 2、扫描找到对应的类
            // 3、并创建对象（Spring中使用反射创建对象class.forName方法）
            doInstance();
            // 4、创建自己的IOC容器
            // 5、将创建好的对象放到自己定义的IOC容器中
            // 6、@Autowrite注解到的对象会在IOC容器中get对应的对象，注入到指定的对象中
            doAutowrite();
            // 7、@RequestMapping上的路径如何映射到对应的控制类和方法上？：：：启动时将控制类中所有路径映射到方法上，绑定起来
            doUrlMapping();
            // 8、接收到请求之后处理请求分发
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理请求分发
     *
     * @param req  请求
     * @param resp 响应
     */
    private void doDispatcherServlet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }

    /**
     * 加载配置文件
     */
    private void doLoadProperties(String contextConfigLocation) {
        Assert.notBlank(contextConfigLocation);
        InputStream input = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation.replace("classpath:", ""));
        try {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 处理URL和方法的映射关系
     */
    private void doUrlMapping() {

    }

    /**
     * url映射 封装成handler对象
     */
    private class Handler {

    }

    /**
     * 扫描到添加了@AutoWrite注解的实例成员
     * 为其注入对象
     */
    private void doAutowrite() throws IllegalAccessException {
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
                    if (field.isAnnotationPresent(MyAutowrite.class)) {
                        MyAutowrite autowrite = field.getAnnotation(MyAutowrite.class);
                        String beanName = autowrite.value();
                        if (StrUtil.isBlank(beanName)) {
                            // 如果没有指定bean名称，就使用变量名
                            beanName = field.getType().getName();
                        }
                        Object needBeAutowrited = iocMap.get(beanName);
                        if (null == needBeAutowrited) {
                            log.warn("警告：bean[{}]为空", beanName);
                        }
                        // 取消Java的权限控制检查 使得private的属性可以被使用
                        field.setAccessible(true);
                        // 向从容器中拿出来的那个bean对象设置注入值
                        field.set(instance, needBeAutowrited);
                        log.info("注入bean[{}]到[{}]成功", beanName, entry.getKey());
                    }
                }
            }
        }
    }

    /**
     * 拿出扫描后存放的所有类名
     * 对添加controller和service的类实例化对象
     * 然后将bean存放在map中
     */
    private void doInstance() throws Exception {
        log.info("开始将对象实例化并放入IOC容器中");
        for (String className : classNames) {
            String cn = className.replace(".class", "");
            // class对象 通过反射拿到注解等信息
            Class<?> clazz = Class.forName(cn);
            if (clazz.isAnnotationPresent(MyController.class)) {
                // 扫描到控制层的类 需要实例化
                Object instance = clazz.newInstance(); // map.put("<bean名称>", instance);
                // 直接把controller上面requestMapping中的值作为key
                String requestMappingValue = clazz.getAnnotation(MyRequestMapping.class).value();
                // 向容器中加入bean
                log.info("放入对象：{}", requestMappingValue);
                iocMap.put(requestMappingValue, instance);
            } else if (clazz.isAnnotationPresent(MyService.class)) {
                Object instance = clazz.newInstance(); // map.put("<bean名称>", instance);
                String requestMappingValue = clazz.getAnnotation(MyService.class).value();
                if (StrUtil.isBlank(requestMappingValue)) {
                    requestMappingValue = StrUtil.lowerFirst(clazz.getCanonicalName());
                }
                // 向容器中加入bean
                log.info("放入对象：{}", requestMappingValue);
                iocMap.put(requestMappingValue, instance);
                Class<?>[] interfaces = clazz.getInterfaces();
                for (Class interfaceI : interfaces) {
                    // 遍历对象 拿到所有的接口 将接口对应的bean也存为instance放入iocMap中
                    log.info("放入对象：{}", interfaceI.getName());
                    iocMap.put(interfaceI.getName(), instance);
                }
            }

        }


    }

    /**
     * 递归扫描指定的包路径下所有文件和文件夹
     * 将所有类名存放起来
     */
    private void doBasePackageScan(String packageScan) throws IOException {
        URL path = this.getClass().getClassLoader().getResource("/" + packageScan.replaceAll("\\.", "/"));
        String fileStr = path.getFile();
        File file = new File(fileStr);
        File[] fileStrs = file.listFiles();
        // 拿出来扫描的所有文件和文件夹 筛选出所有的文件
        for (File thisFile : fileStrs) {
            if (thisFile.isDirectory()) {
                doBasePackageScan(packageScan + "." + thisFile.getName());
            } else {
                // 扫描到class，保存
                String thisClassName = packageScan + "." + thisFile.getName();
                log.info("扫描类：[{}]", thisClassName);
                classNames.add(thisClassName);
            }
        }
    }
}
