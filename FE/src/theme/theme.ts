import { createTheme } from "@mui/material/styles";

export const theme = createTheme({
  typography: {
    fontSize: 18,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          minHeight: 56,
          fontSize: "1.1rem",
        },
      },
    },
  },
});
