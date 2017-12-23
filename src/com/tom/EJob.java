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

	//�û��Զ���classpath�б�  
	private static List<URL> classPath = new ArrayList<URL>();  

	/** 
	 * �����jar�ļ� 
	 * @param root class���ڸ�Ŀ¼ �磺bin 
	 * @return 
	 * @throws IOException 
	 */  
	public static File createTempJar(String root) throws IOException {  
		if (!new File(root).exists()) {  
			return null;  
		}  
		//jar���嵥  
		Manifest manifest = new Manifest();  
		manifest.getMainAttributes().putValue("Manifest-Version", "1.0");  
		//����jar�ļ�  
		// new File(System.getProperty("java.io.tmpdir") ����ϵͳ��ʱ·��
		/**
		 * System.getProperty("java.io.tmpdir") �ǻ�ȡ����ϵͳ�Ļ�����ʱĿ¼
		 *	��windows7�е�Ŀ¼�ǣ�
		 *	
		 *	C:\Users\��¼�û�~1\AppData\Local\Temp\
		 *	
		 *	��linux�µ�Ŀ¼�ǣ�
		 *	
		 *	/tmp  		 
		 * */
		final File jarFile = File.createTempFile("EJob-", ".jar", new File(System.getProperty("java.io.tmpdir")));  
		//�������ɾ��jar  
		Runtime.getRuntime().addShutdownHook(new Thread() {  
			public void run() {  
				jarFile.delete();  
			}  
		});  
		//��jar�ļ���д������  
		JarOutputStream out = new JarOutputStream(new FileOutputStream(jarFile), manifest);  
		createTempJarInner(out, new File(root), "");  
		out.flush();  
		out.close();  
		return jarFile;  
	}  

	/** 
	 * class�ļ���ȡ��д�뵽jar�ļ����ݹ���� 
	 * @param out �����ָ�� 
	 * @param f �ļ�/Ŀ¼���ļ��Ǽ���д���class 
	 * @param base fΪ�ļ�ʱ��base���ļ����·����fΪĿ¼ʱ��base��Ŀ¼ 
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
			//�����µ�class�ļ�����ȡ��д��  
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
	 * ����URL·����ȡclassloader 
	 * @return 
	 */  
	public static ClassLoader getClassLoader() {  
		//��ǰ�̵߳�classloader  
		ClassLoader parent = Thread.currentThread().getContextClassLoader();  
		if (parent == null) {  
			//����classloader  
			parent = EJob.class.getClassLoader();  
		}  
		if (parent == null) {  
			//ϵͳclassloader  
			parent = ClassLoader.getSystemClassLoader();  
		}  
		//����һ��classloader������һ���µ�classloader������һЩclasspath  
		return new URLClassLoader(classPath.toArray(new URL[0]), parent);  
	}  

	/** 
	 * �����ݻ���Ŀ¼��ӵ�classpath 
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