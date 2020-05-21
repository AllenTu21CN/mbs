package com.sanbu.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExtractZip {

	@SuppressWarnings("rawtypes")
	public static List extract(String sZipPathFile, String targetPath) {
		return extract(sZipPathFile, targetPath, false);
	}

	@SuppressWarnings("rawtypes")
	public static List extract(String sZipPathFile, String targetPath, boolean showname) {
		List<String> allFileName = new LinkedList<>();
		try {
			// 先指定压缩档的位置和档名，建立FileInputStream对象
			FileInputStream fins = new FileInputStream(sZipPathFile);
			// 将fins传入ZipInputStream中
			ZipInputStream zins = new ZipInputStream(fins);
			ZipEntry ze = null;
			byte[] ch = new byte[1024];
			//String current=targetPath==null?".":targetPath;
			while ((ze = zins.getNextEntry()) != null) {
				File zfile = new File(targetPath, ze.getName());
				File fpath = new File(zfile.getParentFile().getPath());
				if (ze.isDirectory()) {
					if (!zfile.exists())
						zfile.mkdirs();
					zins.closeEntry();
				} else {
					if (!fpath.exists())
						fpath.mkdirs();
					try(FileOutputStream fouts = new FileOutputStream(zfile)) {
						BufferedOutputStream bufouts = new BufferedOutputStream(fouts);
						int i;
						allFileName.add(zfile.getAbsolutePath());
						while ((i = zins.read(ch)) != -1)
							bufouts.write(ch, 0, i);

						zins.closeEntry();
						bufouts.close();
						fouts.close();
					}
				}
			}
			fins.close();
			zins.close();
		} catch (Exception e) {
			System.err.println("Extract error:" + e.getMessage());
		}
		return allFileName;
	}

	public static void main(String[] args) {
		if(args.length<2){
			System.exit(100);
		}

		String zipFilename = null;
		String targetDir = null;
		boolean showname=false;
		final String k_zipfile = "-f";
		final String k_target = "-d";
		final String k_show="-v";
		int argc = args.length;
		int index = 0;
		while (index < argc) {
			if (args[index].equalsIgnoreCase(k_zipfile)&&index<argc-1) {
				zipFilename = args[++index];
			}

			if (args[index].equalsIgnoreCase(k_target)&&index<argc-1) {
				targetDir = args[++index];
			}

			if(args[index].equalsIgnoreCase(k_show)&&index<argc-1){
				showname=true;
			}
			index++;
		}

		if(zipFilename==null){
			System.out.print("error:no zip file");
			return;
		}

		if(targetDir!=null&&!targetDir.endsWith(File.separator)){
			targetDir+=File.separator;
		}

		File file=new File(zipFilename);
		if(file.exists()&&file.canRead()){
			ExtractZip.extract(zipFilename, targetDir, showname);
		}else{
			System.out.print("error:file not found.");
		}

	}

}
