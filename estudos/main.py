import threading
import time

mem = [1,1]

def fib_task(n):
    time.sleep(0.000001)
    while len(mem) < n:
        mem.append(mem[-1]+mem[-2])
    print(mem[n-1])

input_val = int(input())
spawned_thread = threading.Thread(target=fib_task, args=(input_val,))    
spawned_thread.start()

while len(mem) < input_val-1:
    print("calma")

spawned_thread.join()