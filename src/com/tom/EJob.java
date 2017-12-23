package com.tom;  
import java.io.File;  
import java.io.FileInputStream;  
import java.io.FileOutputStream;  
import java.io.IOException;  
import java.net.URL;  
import java.net.URLClassLoader;  
import java.util.ArrayList;  
import java.util.List;  
import java.util.jar.JarEntry;  
import java.util.jar.JarOutputStream;  
import java.util.jar.Manifest;  

/** 
 * @author tom 
 *  
 */  
public class EJob {  

	//用户自定义classpath列表  
	private static List<URL> classPath = new ArrayList<URL>();  

	/** 
	 * 打包成jar文件 
	 * @param root class所在根目录 如：bin 
	 * @return 
	 * @throws IOException 
	 */  
	public static File createTempJar(String root) throws IOException {  
		if (!new File(root).exists()) {  
			return null;  
		}  
		//jar包清单  
		Manifest manifest = new Manifest();  
		manifest.getMainAttributes().putValue("Manifest-Version", "1.0");  
		//创建jar文件  
		// new File(System.getProperty("java.io.tmpdir") 操作系统临时路径
		/**
		 * System.getProperty("java.io.tmpdir") 是获取操作系统的缓存临时目录
		 *	在windows7中的目录是：
		 *	
		 *	C:\Users\登录用户~1\AppData\Local\Temp\
		 *	
		 *	在linux下的目录是：
		 *	
		 *	/tmp  		 
		 * */
		final File jarFile = File.createTempFile("EJob-", ".jar", new File(System.getProperty("java.io.tmpdir")));  
		//运行完毕删除jar  
		Runtime.getRuntime().addShutdownHook(new Thread() {  
			public void run() {  
				jarFile.delete();  
			}  
		});  
		//向jar文件内写入数据  
		JarOutputStream out = new JarOutputStream(new FileOutputStream(jarFile), manifest);  
		createTempJarInner(out, new File(root), "");  
		out.flush();  
		out.close();  
		return jarFile;  
	}  

	/** 
	 * class文件读取并写入到jar文件，递归调用 
	 * @param out 输出流指向 
	 * @param f 文件/目录，文件是即将写入的class 
	 * @param base f为文件时，base是文件相对路径，f为目录时，base是目录 
	 * @throws IOException 
	 */  
	private static void createTempJarInner(JarOutputStream out, File f,  
			String base) throws IOException {  
		if (f.isDirectory()) {  
			File[] fl = f.listFiles();  
			if (base.length() > 0) {  
				base = base + "/";  
			}  
			for (int i = 0; i < fl.length; i++) {  
				createTempJarInner(out, fl[i], base + fl[i].getName());  
			}  
		} else {  
			//放入新的class文件，读取并写入  
			out.putNextEntry(new JarEntry(base));  
			FileInputStream in = new FileInputStream(f);  
			byte[] buffer = new byte[1024];  
			int n = in.read(buffer);  
			while (n != -1) {  
				out.write(buffer, 0, n);  
				n = in.read(buffer);  
			}  
			in.close();  
		}  
	}  

	/** 
	 * 根据URL路径获取classloader 
	 * @return 
	 */  
	public static ClassLoader getClassLoader() {  
		//当前线程的classloader  
		ClassLoader parent = Thread.currentThread().getContextClassLoader();  
		if (parent == null) {  
			//该类classloader  
			parent = EJob.class.getClassLoader();  
		}  
		if (parent == null) {  
			//系统classloader  
			parent = ClassLoader.getSystemClassLoader();  
		}  
		//基于一个classloader来构建一个新的classloader并加上一些classpath  
		return new URLClassLoader(classPath.toArray(new URL[0]), parent);  
	}  

	/** 
	 * 将内容或者目录添加到classpath 
	 * @param component 
	 */  
	public static void addClasspath(String component) {  

		if ((component != null) && (component.length() > 0)) {  
			try {  
				File f = new File(component);  

				if (f.exists()) {  
					URL key = f.getCanonicalFile().toURI().toURL();  
					if (!classPath.contains(key)) {  
						classPath.add(key);  
					}  
				}  
			} catch (IOException e) {  
				e.printStackTrace();  
			}  
		}  
	}  

	public static void main(String[] args) throws IOException {  
		File jarFile = createTempJar("bin");  



	}  
}