variable "REGISTRY" {
  default = "eco-organic-store"
}

variable "TAG" {
  default = "local"
}

group "default" {
  targets = [
    "gateway",
    "identity-service",
    "catalog-service",
    "cart-service",
    "inventory-service",
    "order-service",
    "payment-service",
    "review-service",
    "frontend"
  ]
}

group "java" {
  targets = [
    "gateway",
    "identity-service",
    "catalog-service",
    "cart-service",
    "inventory-service",
    "order-service",
    "payment-service",
    "review-service"
  ]
}

target "_java" {
  context = "."
  dockerfile = "Dockerfile"
  target = "runtime"
}

target "gateway" {
  inherits = ["_java"]
  args = {
    MODULE = "gateway"
  }
  tags = ["${REGISTRY}/gateway:${TAG}"]
}

target "identity-service" {
  inherits = ["_java"]
  args = {
    MODULE = "identity-service"
  }
  tags = ["${REGISTRY}/identity-service:${TAG}"]
}

target "catalog-service" {
  inherits = ["_java"]
  args = {
    MODULE = "catalog-service"
  }
  tags = ["${REGISTRY}/catalog-service:${TAG}"]
}

target "cart-service" {
  inherits = ["_java"]
  args = {
    MODULE = "cart-service"
  }
  tags = ["${REGISTRY}/cart-service:${TAG}"]
}

target "inventory-service" {
  inherits = ["_java"]
  args = {
    MODULE = "inventory-service"
  }
  tags = ["${REGISTRY}/inventory-service:${TAG}"]
}

target "order-service" {
  inherits = ["_java"]
  args = {
    MODULE = "order-service"
  }
  tags = ["${REGISTRY}/order-service:${TAG}"]
}

target "payment-service" {
  inherits = ["_java"]
  args = {
    MODULE = "payment-service"
  }
  tags = ["${REGISTRY}/payment-service:${TAG}"]
}

target "review-service" {
  inherits = ["_java"]
  args = {
    MODULE = "review-service"
  }
  tags = ["${REGISTRY}/review-service:${TAG}"]
}

target "frontend" {
  context = "frontend"
  dockerfile = "Dockerfile"
  tags = ["${REGISTRY}/frontend:${TAG}"]
}
