
compile_time=`date "+%Y-%m-%d %T"`
tail=`date "+%Y%m%d%T"`
new_version="$1"

export ANDROID_NDK_HOME=/cygdrive/d/dev/android-ndk-r8c
export ANDROID_SDK_HOME=/cygdrive/d/dev/adt-bundle-windows-x86/sdk
export PRJ_HOME_DIR=..
export SRC_DIR=${PRJ_HOME_DIR}/src
export GEN_DIR=${PRJ_HOME_DIR}/gen
export JNI_DIR=${PRJ_HOME_DIR}/jni
export RES_DIR=${PRJ_HOME_DIR}/res
export LIB_DIR=${PRJ_HOME_DIR}/libs
export BUILD_DIR=${PRJ_HOME_DIR}/build
export BIN_DIR=${BUILD_DIR}/bin
export CLASS_OUT_DIR=classes
export ASSET_DIR=${PRJ_HOME_DIR}/assets

export CLASSPATH=${JAVA_HOME}/jre/lib/rt.jar

package=com/zzy/ptt
android_keystore=${BUILD_DIR}/android_ptt.keystore

aapt=${ANDROID_SDK_HOME}/platform-tools/aapt
dx=${ANDROID_SDK_HOME}/platform-tools/dx
apkbuilder=${ANDROID_SDK_HOME}/tools/apkbuilder


android_jar=${ANDROID_SDK_HOME}/platforms/android-8/android.jar

TARGET=zzy_ptt

version_info_file=${ASSET_DIR}/versioninfo.xml

do_aapt()
{
	echo "Generating R.java"
	$aapt package -f -m -J ${GEN_DIR} -M ${PRJ_HOME_DIR}/AndroidManifest.xml -S ${RES_DIR} -A ${ASSET_DIR} -I ${android_jar}

	return $?
}

 do_aidl()
{
	echo "There is nothing to be done for aidl"
}

 do_javac()
{
	echo "Compile All Java Files"
	${JAVA_HOME}/bin/javac -encoding UTF-8 -target 1.6 -bootclasspath ${android_jar} -d ${CLASS_OUT_DIR} ${SRC_DIR}/${package}/ui/*.java ${SRC_DIR}/${package}/db/*.java  ${SRC_DIR}/${package}/exception/*.java ${SRC_DIR}/${package}/jni/*.java ${SRC_DIR}/${package}/model/*.java ${SRC_DIR}/${package}/service/*.java ${SRC_DIR}/${package}/proxy/*.java  ${SRC_DIR}/${package}/util/*.java  ${GEN_DIR}/com/zzy/ptt/R.java

	return $?
}

do_ndk()
{
	echo "Compile libsip-jni.so"
	${ANDROID_NDK_HOME}/ndk-build
	
	return $?
}

do_dx()
{
	echo "do dex"
	$dx --dex --output=${BUILD_DIR}/${TARGET}.dex ${CLASS_OUT_DIR}

	return $?
}

do_second_aapt()
{
	echo "do second aapt"
	$aapt package -f -M ${PRJ_HOME_DIR}/AndroidManifest.xml -S ${RES_DIR} -A ${ASSET_DIR} -I ${android_jar} -F ${BUILD_DIR}/res_pack
}

do_apkbuilder()
{
	echo "do apkbuilder"
	$apkbuilder ${BIN_DIR}/${TARGET}_unsign.apk -u -z ${BUILD_DIR}/res_pack -f ${BUILD_DIR}/${TARGET}.dex -rf ${SRC_DIR} -nf ${LIB_DIR}

	return $?
}

do_signapk()
{
	echo "do signapk"
	${jarsigner} -keystore ${android_keystore} -storepass 123456 -keypass 123456 -signedjar ${BIN_DIR}/${TARGET}_signed.apk ${BIN_DIR}/${TARGET}_unsign.apk android_ptt.keystore
	return $?
}

do_save_build_time_and_version()
{
	echo "do save build time to version.xml";

#save build version	
	if [ -z "${new_version}" ]; then
		echo "no new version info parameter given, maintain the old version"
	else
		echo "new version : ${new_version}"
		sed -i "5c<name>$new_version</name>" ${version_info_file}
	fi
	
#save build time
	sed -i "9c<build_time>$compile_time</build_time>" ${version_info_file}
}


do_clean()
{
    echo "do clean"
    rm res_pack   2>/dev/null
    rm -rf ${CLASS_OUT_DIR}   2>/dev/null
    rm *.dex  2>/dev/null
    rm ${BIN_DIR}/${TARGET}_unsign.apk  2>/dev/null

}

source pre_build.sh
jarsigner=${JAVA_HOME}/bin/jarsigner
do_check_env()
{
    echo "do check env"

	if [ -z $JAVA_HOME ];
	then
		echo "No JAVA_HOME found, Please set JAVA_HOME in pre_build.sh"
		return 1;
	fi
	if [ ! -d "$ANDROID_SDK_HOME" ];
	then
		echo "No ANDROID_SDK_HOME found, please set ANDROID_SDK_HOME and export it"
		return 1;
	fi

	if [ ! -d "$ANDROID_NDK_HOME" ];
	then
		echo "No ANDROID_NDK_HOME found, please set ANDROID_NDK_HOME and export it"
		return 1;
	fi
	mkdir ${CLASS_OUT_DIR}  2>/dev/null
	mkdir ${BIN_DIR} 2>/dev/null
	mkdir ${GEN_DIR} 2>/dev/null
	return 0;
}

do_check_env

if [ $? == 0 ]; then
	echo "ENV check OK"

	do_save_build_time_and_version
	if [ $? == 0 ]; then
		do_aapt
		if [ $? == 0 ]; then
			do_aidl
			if [ $? == 0 ]; then
				do_javac
				if [ $? == 0 ]; then
					do_ndk
					if [ $? == 0 ]; then
						do_dx
						if [ $? == 0 ]; then
							do_second_aapt
							if [ $? == 0 ]; then
								do_apkbuilder
								if [ $? == 0 ]; then
									do_signapk
								fi
							fi
						fi
					fi
				fi
			fi
		fi

	fi

	do_clean

fi






