package com.sma;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pair<T, E> {

	private T key;
	private E value;
}
