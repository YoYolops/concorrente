package main

import (
	"math/rand"
	"fmt"
)

func random_generator(out chan<- int) {
	rand.Seed(42)
	for i:=0; i<rand.Int(); i++ {
		v := rand.Intn(100)
		out <- v
	}
	close(out)
}

func consumidor(in <-chan int, out chan<- int, minVal int, id int) {
	for number := range in {
		if number > minVal {
			fmt.Printf("Consumidor %d recebeu %d\n", id, number)
		}
	}
	close(out)
}

func main() {
	minVal := 50
	ch_1 := make(chan int, 100)
	ch_2 := make(chan int, 100)

	go random_generator(ch_1)
	
	go consumidor(ch_1, ch_2, minVal, 1)
	go consumidor(ch_1, ch_2, minVal, 2)

	for range ch_2 {

	}
}