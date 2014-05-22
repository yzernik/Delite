#include "DeliteCppMemory.h"

size_t DeliteMemorySize = 1024; //48179869184LL;
char *DeliteMemoryBase;
char *DeliteMemoryCurrent;
bool initialized = false;
void DeliteMemoryInit(void) {
	/*
  if(initialized) {
      DeliteMemoryCurrent = DeliteMemoryBase;
      memset(DeliteMemoryBase, 0, DeliteMemorySize);
	  return;
  }
  */
  std::cout << "Initializing the memory " << DeliteMemorySize << std::endl;
  DeliteMemoryBase = (char *)malloc(DeliteMemorySize);
  DeliteMemoryCurrent = DeliteMemoryBase;
  //memset(DeliteMemoryBase, 0, DeliteMemorySize);
  std::cout << "Finished Initializing the memory" << std::endl;
}
void *DeliteMemoryAlloc(size_t bytes) {
	/*
  if(!initialized) {
    DeliteMemoryInit();
    initialized = true;
  }
  */
  size_t alignedSize = (1+ (bytes >> 6)) << 6;
  //std::cout << "Allocating memory " << bytes << "(" << alignedSize << ")" << std::endl;
  void *p = DeliteMemoryCurrent;
  DeliteMemoryCurrent += alignedSize;
  //if(DeliteMemoryCurrent > DeliteMemoryBase + DeliteMemorySize)
	//  assert(false);
  return p;
}

/*
inline void *operator new(size_t bytes) {
  return DeliteMemoryAlloc(bytes); 
}

inline void *operator new[](size_t bytes) {
  return DeliteMemoryAlloc(bytes); 
}

inline void operator delete(void *p) {} 
inline void operator delete[](void *p) {}
*/

