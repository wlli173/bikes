Lucas: 
2. Monitoramento Local (Prometheus ou similar) 

A aplicação Java Spring deve expor métricas através do Spring Boot Actuator e Micrometer (/actuator/prometheus). 

 

No ambiente local, deve subir uma ferramenta de monitoramento (ex: Prometheus) configurada para realizar a coleta (scrape) contínua das métricas expostas pela aplicação. 

 

3. Observabilidade e Dashboards (Grafana) 

Subir uma instância do Grafana integrada ao Prometheus como fonte de dados (Data Source). 

 

Criar e disponibilizar um Dashboard customizado com gráficos exibindo métricas do sistema e da aplicação Java Spring, tais como: 

 
 

Consumo de Memória Heap da JVM. 

 

Uso de CPU da aplicação. 

 

Taxa de requisições HTTP por segundo (RPS). 

 

Contagem e tempo de resposta de requisições por status code (2xx, 4xx, 5xx). 
