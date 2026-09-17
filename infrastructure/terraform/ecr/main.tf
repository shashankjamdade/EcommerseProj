variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

locals {
  services = [
    "api-gateway",
    "auth-service",
    "product-service",
    "inventory-service",
    "cart-service",
    "order-service",
    "payment-service",
    "notification-service"
  ]
}

resource "aws_ecr_repository" "services" {
  for_each = toset(local.services)

  name                 = "${var.project_name}/${each.value}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "${var.project_name}-${each.value}-ecr"
  }
}

resource "aws_ecr_lifecycle_policy" "services" {
  for_each = aws_ecr_repository.services

  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 10 images"
        selection = {
          tagStatus     = "any"
          countType     = "imageCountMoreThan"
          countNumber   = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

output "ecr_repositories" {
  value = {
    for service, repo in aws_ecr_repository.services :
    service => repo.repository_url
  }
}

