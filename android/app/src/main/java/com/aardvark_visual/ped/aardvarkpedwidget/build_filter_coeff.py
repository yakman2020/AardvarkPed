#!/usr/bin/python
import sys

input_file_name = sys.argv[1]


infile = open(input_file_name,  'r')

section_coeff_a0 = {}
section_coeff_a1 = {}
section_coeff_a2 = {}

section_coeff_b0 = {}
section_coeff_b1 = {}
section_coeff_b2 = {}

nth_order_denominator = {}
nth_order_numerator   = {}


lines = infile.readlines()

line_idx = 0
if "2\'nd Order Sections" in lines[line_idx] :
    line_idx = line_idx + 2
    tok = lines[line_idx].split()
    while tok[0] != "Sect":
        line_idx = line_idx + 1
        tok = lines[line_idx].split()

    while tok[0] == "Sect":
        sect_idx = int(tok[1])
        line_idx = line_idx + 1
        tok = lines[line_idx].split()
        if tok[0] == "a0" :
           section_coeff_a0[sect_idx] = tok[1]
           line_idx = line_idx+1 
           tok = lines[line_idx].split()
        if tok[0] == "a1" :
           section_coeff_a1[sect_idx] = tok[1]
           line_idx = line_idx+1 
           tok = lines[line_idx].split()
        if tok[0] == "a2" :
           section_coeff_a2[sect_idx] = tok[1]
           line_idx = line_idx+1 
           tok = lines[line_idx].split()
        if tok[0] == "b0" :
           section_coeff_b0[sect_idx] = tok[1]
           line_idx = line_idx+1 
           tok = lines[line_idx].split()
        if tok[0] == "b1" :
           section_coeff_b1[sect_idx] = tok[1]
           line_idx = line_idx+1 
           tok = lines[line_idx].split()
        if tok[0] == "b2" :
           section_coeff_b2[sect_idx] = tok[1]
           line_idx = line_idx + 1
           tok = lines[line_idx].split()
        while not tok:
           line_idx = line_idx + 1
           tok = lines[line_idx].split()

        while tok[0] != "Sect" and tok[0] != "Nth":
           line_idx = line_idx + 1
           tok = lines[line_idx].split()
          
    while tok[0] != "Nth":
        line_idx = line_idx + 1
        tok = lines[line_idx].split()
        
    if tok[0] == "Nth":
        line_idx = line_idx + 1
        tok = lines[line_idx].split()
        if tok[0] == "Numerator":
            line_idx = line_idx + 1
            tok = lines[line_idx].split()
            while tok[0][0:1] == 'b':
               idx = int(tok[0][1:3])
               nth_order_numerator[idx] = float(tok[1])
               line_idx = line_idx + 1
               tok = lines[line_idx].split()

               # Skip empty lines if any

               while not tok:
                   line_idx = line_idx + 1
                   tok = lines[line_idx].split()
        

        if tok[0] != "Denominator":
            line_idx = line_idx + 1
            tok = lines[line_idx].split()

        if tok[0] == "Denominator":
            line_idx = line_idx + 1
            tok = lines[line_idx].split()
            while tok and tok[0][0:1] == 'a':
               idx = int(tok[0][1:3])
               nth_order_denominator[idx] = float(tok[1])
               line_idx = line_idx + 1
               tok = lines[line_idx].split()


print("    public int NumSections() { return  "+str(len(section_coeff_a0))+"; }; ")
print("")
print("    static  class ChebyshevSectionCoefficients implements SectionCoefficients {")
print("")
print("")
print("        public static SectionCoefficients newInstance()  {")
print("                ChebyshevSectionCoefficients coeff = new ChebyshevSectionCoefficients();")
print("")
print("                return (SectionCoefficients) coeff;")
print("            };")
print("")
print("        @Override")
print("        public final double[] a0() {")
print("                final double[] coeff = {")
for idx in range(len(section_coeff_a0)):
    print("                                                "+section_coeff_a0[idx]+",")
print("                                         };")
print("                   return coeff;")
print("              };")
print("")
print("        @Override")
print("        public final double[] a1() {")
print("                final double[] coeff = {")
for idx in range(len(section_coeff_a1)):
    print("                                                "+section_coeff_a1[idx]+",")
print("                                         };")
print("                   return coeff;")
print("              };")
print("")
print("        @Override")
print("        public final double[] a2() {")
print("                final double[] coeff = {")
for idx in range(len(section_coeff_a2)):
    print("                                                "+section_coeff_a2[idx]+",")
print("                                         };")
print("                   return coeff;")
print("              };")
print("")
print("        @Override")
print("        public final double[] b0() {")
print("                final double[] coeff = {")
for idx in range(len(section_coeff_b0)):
    print("                                                "+section_coeff_b0[idx]+",")
print("                                         };")
print("                   return coeff;")
print("              };")
print("")
print("        @Override")
print("        public final double[] b1() {")
print("                final double[] coeff = {")
for idx in range(len(section_coeff_b1)):
    print("                                                "+section_coeff_b1[idx]+",")
print("                                         };")
print("                   return coeff;")
print("              };")
print("")
print("        @Override")
print("        public final double[] b2() {")
print("                final double[] coeff = {")
for idx in range(len(section_coeff_b2)):
    print("                                                "+section_coeff_b2[idx]+",")
print("                                         };")
print("                   return coeff;")
print("              };")
print("")
print("    };")
print("")
print("   static class ChebyNthOrderCoefficients implements NthOrderCoefficients {")
print("")
print("        @Override")
print("        public final double[] Numerator() {")
print("              final double [] b = { ")
for idx in range(len(nth_order_numerator)):
    print("                                                "+str(nth_order_numerator[idx])+",")
print("                                         };")
print("                   return b;")
print("              };")
print("")
print("")
print("        @Override")
print("        public final double[] Denominator() {")
print("              final double [] a = { ")
for idx in range(len(nth_order_denominator)):
    print("                                                "+str(nth_order_denominator[idx])+",")
print("                                         };")
print("                   return a;")
print("              };")
print("")
print("    };")
print("")
 

        
    
    

