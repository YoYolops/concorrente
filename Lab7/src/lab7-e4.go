package main

import (
	"math/rand"
	"fmt"
)

func random_generator(out chan<- int) {
	rand.Seed(42)
	// Numero aleatorio i pode ser incrivelmente grande, dando a impressão de nunca parar
	for i:=0; i<rand.Int(); i++ {
		v := rand.Intn(100)
		out <- v
	}
	close(out)
}

func consumidor(in <-chan int, out chan<- int, minVal int) {
	for number := range in {
		if number > minVal {
			fmt.Println(number)
		}
	}
	close(out)
}

func main() {
	minVal := 50
	ch_1 := make(chan int, 100)
	ch_2 := make(chan int, 100)

	go random_generator(ch_1)
	go consumidor(ch_1, ch_2, minVal)

	for range ch_2 {
		// Espera até que o consumidor tenha terminado de processar
	}
}