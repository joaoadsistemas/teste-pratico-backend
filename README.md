# Simulador de Crédito - API REST

API para simulação de empréstimos bancários com cálculo de parcelas, juros e valor total baseado na idade do cliente.

## 📋 Requisitos

- Java 17+
- Maven 3.6+
- Docker (opcional)

## 🚀 Setup

### Executando localmente

```bash
# Clonar o repositório
git clone <https://github.com/joaoadsistemas/teste-pratico-backend.git>
cd credit-simulator

# Instalar dependências e executar
mvn clean install
mvn spring-boot:run
```

### Executando com Docker

```bash
# Build e execução
docker-compose build
docker-compose up
```

A aplicação estará disponível em `http://localhost:8080`

## 📚 Documentação da API

Swagger UI disponível em: `http://localhost:8080/swagger-ui.html`

## 🔗 Endpoints

### 1. Simulação Individual

**POST** `/api/v1/simulations`

Calcula parcelas e juros para um empréstimo individual.

**Request:**
```json
{
  "loanAmount": 50000.00,
  "birthDate": "1990-05-15",
  "loanTermMonths": 24
}
```

**Response (200 OK):**
```json
{
  "loanAmount": "50000.00",
  "birthDate": "1990-05-15",
  "clientAge": 35,
  "loanTermMonths": 24,
  "annualInterestRate": "3.0",
  "monthlyPayment": "2146.95",
  "totalAmount": "51526.80",
  "totalInterest": "1526.80"
}
```

### 2. Simulação em Lote

**POST** `/api/v1/simulations/batch`

Processa múltiplas simulações. Até 100 simulações são processadas sincronamente, acima disso o processamento é assíncrono.

**Request (Batch Pequeno - até 100):**
```json
{
  "simulations": [
    {
      "loanAmount": 10000.00,
      "birthDate": "2000-01-01",
      "loanTermMonths": 12
    },
    {
      "loanAmount": 20000.00,
      "birthDate": "1985-06-15",
      "loanTermMonths": 24
    }
  ]
}
```

**Response (200 OK) - Array de resultados:**
```json
[
  {
    "loanAmount": "10000.00",
    "birthDate": "2000-01-01",
    "clientAge": 25,
    "loanTermMonths": 12,
    "annualInterestRate": "5.0",
    "monthlyPayment": "856.07",
    "totalAmount": "10272.84",
    "totalInterest": "272.84"
  },
  {
    "loanAmount": "20000.00",
    "birthDate": "1985-06-15",
    "clientAge": 39,
    "loanTermMonths": 24,
    "annualInterestRate": "3.0",
    "monthlyPayment": "859.75",
    "totalAmount": "20634.00",
    "totalInterest": "634.00"
  }
]
```

**Request (Batch Grande - mais de 100):**
```json
{
  "simulations": [
    ... // 101+ simulações
  ]
}
```

**Response (202 Accepted):**
```json
{
  "batchId": "550e8400-e29b-41d4-a716-446655440000",
  "totalSimulations": 150,
  "status": "ACEITO",
  "message": "Batch recebido e irá ser processado",
  "acceptedAt": "2025-01-24T10:30:00"
}
```

## 📁 Estrutura do Projeto

```
src/main/java/com/spring/credit_simulator/
├── controller/
│   └── SimulationController.java       # Endpoints REST
├── service/
│   ├── SimulationService.java         # Lógica de negócio
│   └── MessageService.java            # Interface para mensageria (abstração)
├── dto/
│   ├── SimulationRequest.java         # DTO de entrada
│   ├── SimulationResponse.java        # DTO de saída
│   ├── BatchSimulationRequest.java    # DTO para lote
│   └── BatchSimulationResponse.java   # DTO resposta do lote
├── exception/
│   ├── ValidationException.java       # Exceção customizada
│   └── GlobalExceptionHandler.java    # Tratamento global de erros
├── util/
│   └── LoanCalculator.java           # Cálculos financeiros
└── config/
    ├── SwaggerConfig.java            # Configuração da documentação
    └── ThreadPoolConfig              # Componente de ciclo da Thread
```

## 🏗️ Decisões de Arquitetura

### 1. **Estrutura em Camadas**
- **Controller**: Gerencia requisições HTTP e respostas
- **Service**: Contém toda lógica de negócio
- **Util**: Isola cálculos complexos para facilitar testes
- **DTO**: Separa representação externa da lógica interna

### 2. **Cálculos Financeiros**
- Uso de `BigDecimal` para precisão monetária
- Fórmula de parcelas fixas (Sistema Price)
- Arredondamento HALF_UP com 2 casas decimais

### 3. **Processamento de Lotes**
- **Síncrono** (≤100): Usa `CompletableFuture` com thread pool
- **Assíncrono** (>100): Retorna ID para rastreamento
- Thread pool configurável via properties

### 4. **Validações**
- Bean Validation nos DTOs
- Validações de negócio no Service
- Tratamento centralizado de exceções

### 5. **Mensageria (Abstração)**
- Interface `MessageService` permite diferentes implementações
- Em produção: RabbitMQ, Kafka ou SQS
- Desacoplamento entre lógica e infraestrutura

### 6. **Taxas de Juros por Idade**
| Faixa Etária | Taxa Anual |
|--------------|------------|
| Até 25 anos | 5% |
| 26 a 40 anos | 3% |
| 41 a 60 anos | 2% |
| Acima de 60 | 4% |

## 🧪 Executando Testes

```bash
# Todos os testes
mvn test

# Apenas testes unitários
mvn test -Dtest="*Test"

# Apenas testes de integração
mvn test -Dtest="*IntegrationTest"
```

## ⚙️ Configurações

### application.properties
```properties
# Porta do servidor
server.port=8080

# Debug Info
logging.level.root=INFO
logging.level.com.empresa.creditsimulator=DEBUG

# Jackson formatação
spring.jackson.default-property-inclusion=non_null
spring.jackson.date-format=yyyy-MM-dd

# Swagger
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
```

## 📝 Observações

- Cliente deve ter no mínimo 18 anos
- Valor do empréstimo: R\$ 1.000 a R$ 1.000.000
- Prazo: 6 a 360 meses
- Processamento assíncrono abstrato (sem implementação real de mensageria)