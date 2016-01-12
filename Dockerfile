FROM parana/java-jdk9
#
# Esta imagem da qual herdo as funcionalidades inclui Java OpenJDK 9 
# build 96 disponível em https://jdk9.java.net/download/ 
# com suporte a REPL via jshell
#
MAINTAINER João Antonio Ferreira "joao.parana@gmail.com"

ENV REFRESHED_AT 2016-01-12

ENV OPENDSB_HOME  /usr/local/opendsb

WORKDIR $OPENDSB_HOME

# Para compilar fontes Java com encoding UTF-8
# ENV JAVA_TOOL_OPTIONS "-Dfile.encoding=UTF8"

# ENTRYPOINT ["/config-or-run-app"]
CMD ["/bin/bash"]
