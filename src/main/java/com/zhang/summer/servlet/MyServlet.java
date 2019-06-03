package com.zhang.summer.servlet;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.zhang.summer.annotation.*;
import lombok.Getter;
import lombok.Setter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            // 2、创建对象（Spring中使用反射创建对象class.forName方法）
            doInstance();
            // 3、创建自己的IOC容器
            // 将创建好的对象放到自己定义的IOC容器中
            // @Autowrite注解到的对象会在IOC容器中get对应的对象，注入到指定的对象中
            doAutowrite();
            // 4、@RequestMapping上的路径如何映射到对应的控制类和方法上？：：：启动时将控制类中所有路径映射到方法上，绑定起来
            doUrlMapping();
            // 5、接收到请求之后处理请求分发 在doGet或者doPost完成
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
        try {
            Handler handler = getHandler(req);
            if (null == handler) {
                log.warn("404 not found url mapping==> {}", req.getRequestURI());
                resp.getWriter().write("404 not found");
                return;
            }
            Class<?>[] paramTypes = handler.method.getParameterTypes();
            Object[] paramValues = new Object[paramTypes.length];
            Map<String, String[]> params = req.getParameterMap();
            for (Map.Entry<String, String[]> param : params.entrySet()) {
                String value = Arrays.toString(param.getValue());
                value = value.replaceAll("\\[|\\]", "");
                if (handler.paramIndexMapping.containsKey(param.getKey())) {
                    int index = handler.paramIndexMapping.get(param.getKey());
                    paramValues[index] = convert(paramTypes[index], value);
                }
            }
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[reqIndex] = req;
            paramValues[respIndex] = resp;
            Object ret = handler.method.invoke(handler.controller, paramValues);
            resp.setCharacterEncoding("utf-8");
            if (handler.method.getClass().isAnnotationPresent(MyResponsebody.class)) {
                resp.setContentType("application/json; charset=utf-8");
                //拼接json数据
                String jsonStr = JSONUtil.toJsonStr(ret);
                //将数据写入流中
                resp.getWriter().write(jsonStr);
            }else{
                resp.setContentType("application/json; charset=utf-8");
                //将数据写入流中
                resp.getWriter().write(ret.toString());
            }
            log.info("doDispatcherServlet方法执行完成，返回值：{}", ret);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将整型变量转换一下
     */
    private Object convert(Class clazz, String value) {
        if (clazz == Integer.class) {
            return Integer.valueOf(value);
        }
        return value;
    }

    /**
     * 通过请求的URL 获取到handler对象
     * 其中会有要执行的方法以及参数名对应的下标
     */
    private Handler getHandler(HttpServletRequest req) {
        if (!CollectionUtil.isEmpty(handlerList)) {
            String url = req.getRequestURI();
            log.info("get request mapping ==> {}", url);
            String contextPath = req.getContextPath();
            String realPath = url.replaceAll(contextPath, "").replaceAll("/+", "/");
            for (Handler handler : handlerList) {
                Matcher matcher = handler.pattern.matcher(realPath);
                if (matcher.matches()) {
                    return handler;
                }
            }
        }
        return null;
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
        // 遍历所有bean如果是controller在进行处理
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Object bean = entry.getValue();
            if (bean.getClass().isAnnotationPresent(MyController.class)) {
                String beanName = entry.getKey();
                Method[] methods = bean.getClass().getMethods();
                for (Method n : methods) {
                    if (n.isAnnotationPresent(MyRequestMapping.class)) {
                        String mUrl = beanName + n.getAnnotation(MyRequestMapping.class).value();
                        if (StrUtil.isBlank(mUrl)) {
                            log.error("方法未声明URL");
                            continue;
                        }
                        Pattern pattern = Pattern.compile(mUrl);
                        handlerList.add(new Handler(pattern, bean, n));
                        log.info("Add Mapping --- URL: {}, method: {}", mUrl, n.getName());
                    }
                }
            }
        }

    }

    /**
     * url映射 封装成handler对象
     */
    @Getter
    @Setter
    private class Handler {
        private Object controller;
        private Pattern pattern;
        private Method method;
        // 使用map存放方法参数对应下标 即第几个参数是什么名字
        private Map<String, Integer> paramIndexMapping;

        protected Handler(Pattern pattern, Object controller, Method method) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            paramIndexMapping = initParamIndexMapping(method);
        }

        private Map<String, Integer> initParamIndexMapping(Method method) {
            Map<String, Integer> map = new HashMap<String, Integer>();
            Annotation[][] annotations = method.getParameterAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                for (Annotation annotation : annotations[i]) {
                    if (annotation instanceof MyRequestParam) {
                        String paramName = ((MyRequestParam) annotation).value();
                        if (StrUtil.isNotBlank(paramName)) {
                            map.put(paramName, i);
                        }
                    }
                }
            }
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int j = 0; j < paramTypes.length; j++) {
                Class<?> paramType = paramTypes[j];
                if (paramType == HttpServletRequest.class || paramType == HttpServletResponse.class) {
                    map.put(paramType.getName(), j);
                }
            }
            return map;
        }

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
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    // 判断哪些需要注入
                    if (field.isAnnotationPresent(MyAutowrite.class)) {
                        MyAutowrite autowrite = field.getAnnotation(MyAutowrite.class);
                        String beanName = autowrite.value();
                        if (StrUtil.isBlank(beanName)) {
                            // 如果没有指定bean名称，就使用变量名
                            beanName = StrUtil.lowerFirst(field.getType().getSimpleName());
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
