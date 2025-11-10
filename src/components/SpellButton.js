import React, { useState, useEffect } from "react";
import { TouchableOpacity, Text, StyleSheet } from "react-native";

export default function SpellButton({ spell }) {
  const [timeLeft, setTimeLeft] = useState(0);

  useEffect(() => {
    if (timeLeft <= 0) return;
    const interval = setInterval(() => {
      setTimeLeft((t) => t - 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [timeLeft]);

  const startCooldown = () => setTimeLeft(spell.cooldown);

  return (
    <TouchableOpacity style={styles.button} onPress={startCooldown}>
      <Text style={styles.text}>
        {spell.name} {timeLeft > 0 ? `(${timeLeft}s)` : ""}
      </Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  button: {
    margin: 10,
    padding: 20,
    backgroundColor: "#4a90e2",
    borderRadius: 8,
  },
  text: {
    color: "#fff",
    fontSize: 18,
    textAlign: "center",
  },
});
