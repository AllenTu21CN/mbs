#!/bin/sh 
#-----------------------------------------------------------------------
# Fuction: configure.sh
# Version: 1.0
# Created: Tuyj
# Created date:2017/04/13
# Parameters:
#   $1 -- Project cmake building parent directory
#-----------------------------------------------------------------------

if [ ! $# -eq 1 ]
then
    echo "Input error. Usage:"
    echo "  \$1  -- Project cmake building parent directory (It looks like <ProjectDir>/module_multimedia/.externalNativeBuild/cmake.custom/debug)"
    exit 1
fi 

    
echo "Search cmake_build_command.sh in '$1'"

for building_script in  `find $1 -name cmake_build_command.sh`
do
    echo ---- Call "$building_script"
    sh "$building_script"
done

    
    
    
    