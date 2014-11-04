JAVA=java
JAVAC=javac
LIBS=-cp ./json-simple-1.1.1.jar:.
OBJ=Client.class \
	Server.class \
	ExecSimulation.class
CONF_DIR=../config/

.PHONY: all clean %
all: $(OBJ)

$(OBJ): %.class: %.java
		$(JAVAC) $(LIBS) $<

clean:
	rm *.class

test-%: all
	$(JAVA) $(LIBS) ExecSimulation $(CONF_DIR)$(subst test-,,$@)
