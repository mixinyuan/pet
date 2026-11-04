package com.pawbites.petshop.result;

import java.util.List;

public record PageResult<T>(List<T> records, long total, long page, long size) {
}
