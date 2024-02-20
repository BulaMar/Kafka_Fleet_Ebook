package pl.jlabs.ebook;

public enum City {
  // Germany
  BERLIN("Germany"),
  MUNICH("Germany"),
  HAMBURG("Germany"),
  FRANKFURT("Germany"),
  COLOGNE("Germany"),

  // Slovakia
  BRATISLAVA("Slovakia"),
  KOSICE("Slovakia"),
  PRESOV("Slovakia"),
  ZILINA("Slovakia"),
  NITRA("Slovakia"),

  // France
  PARIS("France"),
  MARSEILLE("France"),
  LYON("France"),
  TOULOUSE("France"),
  NICE("France"),

  // Czech Republic
  PRAGUE("Czech Republic"),
  BRNO("Czech Republic"),
  OSTRAVA("Czech Republic"),
  PLZEN("Czech Republic"),
  LIBEREC("Czech Republic"),

  // Poland
  KRAKOW("Poland"),
  WARSAW("Poland"),
  WROCLAW("Poland"),
  POZNAN("Poland"),
  GDANSK("Poland");

  private final String country;

  City(String country) {
    this.country = country;
  }

  public String getCountry() {
    return country;
  }
}
