#ifndef __DELITE_CPP_H__
#define __DELITE_CPP_H__

#include <cstdarg>
#include <iostream>
#include <fstream>
#include <algorithm> 
#include <functional> 
#include <cctype>
#include <locale>
#include <assert.h>
#include <string>
#include <regex>
#include <vector>
#include <map>
#include <sstream>
#include "cppDeliteArray.h"

/* helper methods and data structures only required for execution with Delite */
#ifndef __DELITE_CPP_STANDALONE__
#include <jni.h>
#endif

using namespace std;

cppDeliteArray< string >* string_split(string str, string pattern);
int32_t string_toInt(string str);
float string_toFloat(string str);
double string_toDouble(string str);
bool string_toBoolean(string str);
string string_trim(string str);
int8_t string_charAt(string str, int idx);
bool string_startsWith(string str, string substr);
string string_plus(string str1, string str2);
cppDeliteArray< string > *cppArgsGet(int num, ...);
template<class T> string convert_to_string(T in);
string readFirstLineFile(string filename);

#ifndef __DELITE_CPP_STANDALONE__
extern std::map<int,jobject> *JNIObjectMap;
jobject JNIObjectMap_find(int key);
void JNIObjectMap_insert(int key, jobject value);
#endif

#endif
