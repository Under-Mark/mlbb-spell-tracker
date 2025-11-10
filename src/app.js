import React from "react";
import { View, Text, StyleSheet } from "react-native";
import { spells } from "./src/data/spells";
import SpellButton from "./src/components/SpellButton";

export default function App() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>MLBB Spell Tracker</Text>
      {spells.map((spell) => (
        <SpellButton key={spell.name} spell={spell} />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#222",
  },
  title: {
    fontSize: 24,
    color: "#fff",
    marginBottom: 20,
  },
});
