#ifndef __DELITE_CPP_MEMORY_H__
#define __DELITE_CPP_MEMORY_H__

#include <iostream>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>

using namespace std;

void DeliteMemoryInit(void);
void *DeliteMemoryAlloc(size_t bytes);

/*
inline void *operator new(size_t bytes);
inline void *operator new[](size_t bytes);
inline void operator delete(void *p);
inline void operator delete[](void *p);
*/

#endif
