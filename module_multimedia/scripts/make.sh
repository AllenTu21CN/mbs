#-----------------------------------------------------------------------
# Fuction: make.sh
# Version: 1.0
# Created: Tuyj
# Created date:2017/04/13
# Parameters:
#   $1 -- Project cmake building parent directory
#   $2 -- Make program
#-----------------------------------------------------------------------

if [ ! $# -eq 2 ]
then
    echo "Input error. Usage:"
    echo "  \$1  -- Project cmake building parent directory (It looks like <ProjectDir>/module_multimedia/.externalNativeBuild/cmake.custom/debug)"
    echo "  \$2  -- Make program. (It looks like ^<SDK_HOME^>/cmake/^<version^>/bin/ninja)"
    exit 1
fi


for target in `ls -F $1 |grep /$`
do
    echo ---- Call "$2" -C "$1/$target"
    "$2" -C "$1/$target"
done

