package main

import (
	"math/rand"
	"fmt"
)

func random_generator_1(out chan int) {
	rand.Seed(42)
	for i:=0; i<rand.Int(); i++ {
		v := rand.Intn(100)
		out <- v
	}
	close(out)
}

func random_generator_2(out chan int) {
	rand.Seed(42)
	for i:=0; i<rand.Int(); i++ {
		v := rand.Intn(100)
		out <- v
	}
	close(out)
}


func main() {
	minVal := 50
	ch_1 := make(chan int)
	ch_2 := make(chan int)
	
	go random_generator_1(ch_1)
	go random_generator_2(ch_2)

	for {
		number_1 := <-ch_1
		if number_1 > minVal {
			fmt.Println(number_1)
		}
		number_2 := <-ch_2
		if number_2 > minVal {
			fmt.Println(number_2)
		}
	}
}