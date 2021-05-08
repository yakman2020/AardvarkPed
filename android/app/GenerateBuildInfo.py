
# 
#    Copyright (c) 2014-2015 Aardvark Visual Software Inc,
#  
#    All Rights Reserved.
#  
#  
#  

#!/usr/bin/python
import time;

srcDir="./src/main/java/com/aardvark_visual/ped/aardvarkpedanalysis/"
#srcDir="./"
build_id_format= "Build-aardvarkped-analysis-%S%M%H%d%m%Y"

f = open(srcDir+"BuildInfo.java", "w")
f.write("/* Generated Source>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n")
f.write("                                                     */\n")
f.write("package com.aardvark_visual.ped;\n")
f.write("public class BuildInfo {\n")
f.write(    "public static String BUILD_ID = \""+time.strftime(build_id_format)+"\";\n");
f.write("}\n")
f.write("\n")



